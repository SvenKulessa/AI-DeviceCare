package com.example.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devicecare.PrivacySummaryReport
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.permissions.InstalledAppsScanResult
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAdvisorViewModel"
private const val GEMINI_MODEL_NAME = "gemini-3.8-flash"
private const val REQUEST_TIMEOUT_MILLIS = 60_000L

sealed interface AiUiState {
  data object Idle : AiUiState

  data object Loading : AiUiState

  data class Streaming(val text: String) : AiUiState

  data class Success(val text: String) : AiUiState

  data class Error(val message: String) : AiUiState
}

data class AiChatMessage(
  val id: Long,
  val isUser: Boolean,
  val text: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)

data class AiAdvisorUiState(
  val messages: List<AiChatMessage> = emptyList(),
  val status: AiUiState = AiUiState.Idle,
  val includeAnonymizedContext: Boolean = false,
) {
  val isGenerating: Boolean
    get() = status is AiUiState.Loading || status is AiUiState.Streaming
}

class AiAdvisorViewModel internal constructor(
  private val privacyAnalyzer: PermissionPrivacyAnalyzerService,
  private val nextStepsService: PostureNextStepsService = GeminiPostureNextStepsService(),
) : ViewModel() {

  constructor() : this(GeminiPermissionPrivacyAnalyzerService(), GeminiPostureNextStepsService())

  private val messageIdGen = AtomicLong(1L)

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = GEMINI_MODEL_NAME,
      systemInstruction =
        content {
          text(
            "Du bist der integrierte KI-Berater von AI-DeviceCare für Android-Smartphones. " +
              "Beantworte Fragen zu Android-Akku-Telemetrie, Energiespareinstellungen, " +
              "Laufzeitberechtigungen (z. B. Kamera, Standort, Mikrofon) und Gerätesicherheit " +
              "präzise, sachlich und auf Deutsch. Erfinde niemals Messwerte oder Diagnose-Scores."
          )
        },
    )
  }

  private var chatSession: Chat? = null

  private val _uiState = MutableStateFlow(AiAdvisorUiState())
  val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

  private val _privacyAnalysisState =
    MutableStateFlow<PermissionPrivacyAnalysisState>(PermissionPrivacyAnalysisState.Idle)
  val privacyAnalysisState: StateFlow<PermissionPrivacyAnalysisState> =
    _privacyAnalysisState.asStateFlow()

  private val _postureNextStepsState =
    MutableStateFlow<PostureNextStepsState>(PostureNextStepsState.Idle)
  val postureNextStepsState: StateFlow<PostureNextStepsState> =
    _postureNextStepsState.asStateFlow()

  fun analyzePermissionsPrivacy(scanResult: InstalledAppsScanResult?) {
    if (scanResult == null || scanResult.errorMessage != null) {
      _privacyAnalysisState.value =
        PermissionPrivacyAnalysisState.Error(
          "Bitte zuerst einen lokalen App-Scan durchführen, bevor Gemini analysiert."
        )
      return
    }
    if (_privacyAnalysisState.value is PermissionPrivacyAnalysisState.Loading) return

    _privacyAnalysisState.value = PermissionPrivacyAnalysisState.Loading
    viewModelScope.launch {
      try {
        val report =
          withTimeout(REQUEST_TIMEOUT_MILLIS) {
            privacyAnalyzer.analyzeGrantedPermissions(scanResult)
          }
        _privacyAnalysisState.value = PermissionPrivacyAnalysisState.Success(report)
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Permission privacy analysis timed out", e)
        _privacyAnalysisState.value =
          PermissionPrivacyAnalysisState.Error(
            "Zeitüberschreitung nach 60 Sekunden. Bitte Verbindung prüfen und erneut versuchen."
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Permission privacy analysis failed", e)
        _privacyAnalysisState.value =
          PermissionPrivacyAnalysisState.Error(
            e.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "Datenschutz-Analyse mit Gemini fehlgeschlagen."
          )
      }
    }
  }

  fun clearPrivacyAnalysis() {
    _privacyAnalysisState.value = PermissionPrivacyAnalysisState.Idle
  }

  fun generatePostureNextSteps(
    summaryReport: PrivacySummaryReport,
    batterySnapshot: BatteryDashboardSnapshot? = null,
  ) {
    if (_postureNextStepsState.value is PostureNextStepsState.Loading) return

    _postureNextStepsState.value = PostureNextStepsState.Loading
    viewModelScope.launch {
      try {
        val report =
          withTimeout(REQUEST_TIMEOUT_MILLIS) {
            nextStepsService.analyzeDevicePosture(summaryReport, batterySnapshot)
          }
        _postureNextStepsState.value = PostureNextStepsState.Success(report)
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Posture next-steps generation timed out", e)
        _postureNextStepsState.value =
          PostureNextStepsState.Error(
            "Zeitüberschreitung nach 60 Sekunden. Bitte Verbindung prüfen und erneut versuchen."
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Posture next-steps generation failed", e)
        _postureNextStepsState.value =
          PostureNextStepsState.Error(
            e.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "Gemini-Auswertung des Gerätestatus fehlgeschlagen."
          )
      }
    }
  }

  fun clearPostureNextSteps() {
    _postureNextStepsState.value = PostureNextStepsState.Idle
  }

  fun toggleIncludeAnonymizedContext() {
    _uiState.update { it.copy(includeAnonymizedContext = !it.includeAnonymizedContext) }
  }

  fun clearChat() {
    chatSession = null
    _uiState.update {
      it.copy(
        messages = emptyList(),
        status = AiUiState.Idle,
      )
    }
  }

  fun dismissError() {
    _uiState.update {
      if (it.status is AiUiState.Error) it.copy(status = AiUiState.Idle) else it
    }
  }

  fun sendPrompt(
    rawPrompt: String,
    batterySnapshot: BatteryDashboardSnapshot? = null,
    appsScanResult: InstalledAppsScanResult? = null,
  ) {
    val trimmed = rawPrompt.trim()
    if (trimmed.isEmpty() || _uiState.value.isGenerating) return

    val userMessage =
      AiChatMessage(
        id = messageIdGen.getAndIncrement(),
        isUser = true,
        text = trimmed,
      )

    val modelPrompt =
      if (_uiState.value.includeAnonymizedContext) {
        val contextBlock = buildAnonymizedContextSummary(batterySnapshot, appsScanResult)
        if (contextBlock != null) {
          "$trimmed\n\n[Anonymisierter Gerätekontext ohne Paketnamen]\n$contextBlock"
        } else {
          trimmed
        }
      } else {
        trimmed
      }

    _uiState.update {
      it.copy(
        messages = it.messages + userMessage,
        status = AiUiState.Loading,
      )
    }

    viewModelScope.launch {
      val accumulated = StringBuilder()
      try {
        val activeChat =
          chatSession
            ?: model.startChat().also { created ->
              chatSession = created
            }

        withTimeout(REQUEST_TIMEOUT_MILLIS) {
          activeChat.sendMessageStream(modelPrompt).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            if (chunkText.isNotEmpty()) {
              accumulated.append(chunkText)
              _uiState.update { it.copy(status = AiUiState.Streaming(accumulated.toString())) }
            }
          }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isNotEmpty()) {
          val reply =
            AiChatMessage(
              id = messageIdGen.getAndIncrement(),
              isUser = false,
              text = finalText,
            )
          _uiState.update {
            it.copy(
              messages = it.messages + reply,
              status = AiUiState.Success(finalText),
            )
          }
        } else {
          _uiState.update {
            it.copy(status = AiUiState.Error("Das Gemini-Modell hat eine leere Antwort geliefert."))
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini streaming request timed out", e)
        _uiState.update {
          it.copy(
            status =
              AiUiState.Error(
                "Zeitüberschreitung nach 60 Sekunden. Bitte Verbindung prüfen und erneut versuchen."
              )
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI Logic request failed", e)
        _uiState.update {
          it.copy(
            status =
              AiUiState.Error(
                e.localizedMessage?.takeIf { msg -> msg.isNotBlank() }
                  ?: "Gemini-Anfrage fehlgeschlagen."
              )
          )
        }
      }
    }
  }

  companion object {
    /**
     * Builds strictly anonymized numeric/enum context (no package names, no device IDs, no PII).
     */
    fun buildAnonymizedContextSummary(
      battery: BatteryDashboardSnapshot?,
      appsScan: InstalledAppsScanResult?,
    ): String? {
      val parts = mutableListOf<String>()
      if (battery != null && battery.errorMessage == null) {
        parts +=
          buildString {
            append("Akku: ")
            append(battery.percent?.let { "$it%" } ?: "N/A")
            append(", Status=${battery.chargingStatus.label}")
            append(", Quelle=${battery.powerSource.label}")
            append(", Zustand=${battery.healthCondition.label}")
            battery.temperatureCelsius?.let { append(", Temp=${String.format("%.1f", it)}°C") }
            append(", Restzeit=${battery.remainingTime.formattedDuration}")
            battery.powerSaveMode?.let { append(", Sparmodus=$it") }
          }
      }
      if (appsScan != null && appsScan.errorMessage == null) {
        parts +=
          "Berechtigungs-Aggregate: sichtbareApps=${appsScan.visiblePackageCount}, " +
            "sensibleApps=${appsScan.appsWithHighSensitivityCount}, " +
            "Kamera=${appsScan.cameraAccessCount}, " +
            "Standort=${appsScan.locationAccessCount}, " +
            "Mikrofon=${appsScan.microphoneAccessCount}"
      }
      return if (parts.isEmpty()) null else parts.joinToString(separator = "\n")
    }
  }
}
