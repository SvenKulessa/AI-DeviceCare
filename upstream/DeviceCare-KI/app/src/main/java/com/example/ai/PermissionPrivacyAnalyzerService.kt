package com.example.ai

import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val PRIVACY_ANALYZER_MODEL_NAME = "gemini-3.8-flash"
private const val PRIVACY_ANALYSIS_TIMEOUT_MILLIS = 60_000L

enum class PrivacyRecommendationPriority(val label: String) {
  HIGH("HOCH"),
  MEDIUM("MITTEL"),
  LOW("NIEDRIG"),
}

data class PrivacySuggestion(
  val title: String,
  val priority: PrivacyRecommendationPriority,
  val recommendation: String,
  val targetPermission: String?,
  val affectedApps: List<String>,
)

data class PrivacyAnalysisReport(
  val summary: String,
  val keyRisks: List<String>,
  val suggestions: List<PrivacySuggestion>,
  val analyzedAppCount: Int,
  val analyzedPermissionCount: Int,
  val omittedDangerousAppCount: Int = 0,
  val generatedAtMillis: Long = System.currentTimeMillis(),
)

sealed interface PermissionPrivacyAnalysisState {
  data object Idle : PermissionPrivacyAnalysisState

  data object Loading : PermissionPrivacyAnalysisState

  data class Success(val report: PrivacyAnalysisReport) : PermissionPrivacyAnalysisState

  data class Error(val message: String) : PermissionPrivacyAnalysisState
}

data class PermissionDigestPayload(
  val promptText: String,
  val analyzedAppCount: Int,
  val analyzedPermissionCount: Int,
  val omittedDangerousAppCount: Int,
)

interface PermissionPrivacyAnalyzerService {
  suspend fun analyzeGrantedPermissions(scanResult: InstalledAppsScanResult): PrivacyAnalysisReport
}

class GeminiPermissionPrivacyAnalyzerService : PermissionPrivacyAnalyzerService {

  private val suggestionSchema =
    Schema.obj(
      properties =
        mapOf(
          "title" to
            Schema.string(
              description = "Kurzer, handlungsorientierter Titel der Datenschutz-Empfehlung"
            ),
          "priority" to
            Schema.enumeration(
              values = listOf("HIGH", "MEDIUM", "LOW"),
              description = "Priorität der Empfehlung basierend auf dem Datenschutz-Risiko",
            ),
          "recommendation" to
            Schema.string(
              description =
                "Verständliche, konkrete Schritt-für-Schritt-Empfehlung in natürlicher deutscher Sprache"
            ),
          "targetPermission" to
            Schema.string(
              description =
                "Relevanter Berechtigungsname wie CAMERA, ACCESS_FINE_LOCATION, RECORD_AUDIO oder null",
              nullable = true,
            ),
          "affectedApps" to
            Schema.array(
              items = Schema.string(),
              description = "Namen der betroffenen Apps aus der übergebenen Liste",
            ),
        ),
      optionalProperties = listOf("targetPermission"),
    )

  private val reportSchema =
    Schema.obj(
      properties =
        mapOf(
          "summary" to
            Schema.string(
              description =
                "Natürliche, sachliche Gesamteinschätzung der gewährten Berechtigungen auf Deutsch (2 bis 4 Sätze)"
            ),
          "keyRisks" to
            Schema.array(
              items = Schema.string(),
              description = "2 bis 5 knappe Stichpunkte zu den wichtigsten Datenschutz-Risiken",
            ),
          "suggestions" to
            Schema.array(
              items = suggestionSchema,
              description =
                "Konkrete, priorisierte Vorschläge zur Verbesserung der Geräte-Privatsphäre",
            ),
        )
    )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = PRIVACY_ANALYZER_MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = reportSchema
        },
      systemInstruction =
        content {
          text(
            "Du bist ein spezialisierter Android-Datenschutzberater in AI-DeviceCare. " +
              "Analysiere ausschließlich die übergebene Liste gewährter gefährlicher Laufzeitberechtigungen " +
              "und liefere präzise, praxisnahe und ruhige Empfehlungen auf Deutsch. " +
              "Wichtig: Eine gewährte Berechtigung ist kein Malware-Beweis; erkläre sachlich, warum eine " +
              "Kombination (z. B. Hintergrundstandort, Kamera, Mikrofon, Kontakte/SMS) prüfenswert ist " +
              "und welche Einschränkung in den Android-App-Einstellungen sinnvoll sein kann " +
              "(z. B. 'Nur während der Nutzung' oder Entziehen). " +
              "Erfinde niemals Apps oder Berechtigungen, die nicht in den Eingabedaten stehen."
          )
        },
    )
  }

  override suspend fun analyzeGrantedPermissions(
    scanResult: InstalledAppsScanResult
  ): PrivacyAnalysisReport {
    val digest = buildPermissionDigestForGemini(scanResult)
    val response =
      withTimeout(PRIVACY_ANALYSIS_TIMEOUT_MILLIS) { model.generateContent(digest.promptText) }
    val jsonString =
      response.text?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Gemini hat eine leere Datenschutz-Analyse zurückgegeben.")
    return parsePrivacyAnalysisJson(
      jsonString = jsonString,
      analyzedAppCount = digest.analyzedAppCount,
      analyzedPermissionCount = digest.analyzedPermissionCount,
      omittedDangerousAppCount = digest.omittedDangerousAppCount,
    )
  }

  companion object {
    fun buildPermissionDigestForGemini(
      scanResult: InstalledAppsScanResult,
      maxApps: Int = 40,
    ): PermissionDigestPayload {
      val appsWithDangerous =
        scanResult.apps
          .filter { it.grantedDangerousPermissions.isNotEmpty() }
          .sortedWith(
            compareByDescending<InstalledAppPermissionInfo> { it.criticalSensorTags.size }
              .thenByDescending { it.hasHighSensitivityAccess }
              .thenBy { it.isSystemApp }
              .thenByDescending { it.grantedDangerousPermissions.size }
              .thenBy { it.appName.lowercase() }
          )

      val selectedApps = appsWithDangerous.take(maxApps.coerceAtLeast(1))
      val omittedCount = (appsWithDangerous.size - selectedApps.size).coerceAtLeast(0)
      val permissionCount = selectedApps.sumOf { it.grantedDangerousPermissions.size }

      val promptText = buildString {
        appendLine("Analysiere die folgende Momentaufnahme gewährter Android-Laufzeitberechtigungen:")
        appendLine("- Sichtbare installierte Apps gesamt: ${scanResult.visiblePackageCount}")
        appendLine("- Apps mit gefährlichen Laufzeitrechten: ${scanResult.appsWithDangerousCount}")
        appendLine(
          "- Apps mit hochsensiblen Zugriffen: ${scanResult.appsWithHighSensitivityCount} " +
            "(Kamera=${scanResult.cameraAccessCount}, Standort=${scanResult.locationAccessCount}, Mikrofon=${scanResult.microphoneAccessCount})"
        )
        appendLine()
        if (selectedApps.isEmpty()) {
          appendLine(
            "Aktuell sind keiner sichtbaren App gefährliche Laufzeitberechtigungen gewährt. " +
              "Bestätige diese Ausgangslage und gib 2 bis 3 allgemeine, hilfreiche Android-Datenschutz-Tipps."
          )
        } else {
          appendLine("Apps mit gewährten gefährlichen Berechtigungen:")
          selectedApps.forEach { app ->
            val originLabel = if (app.isSystemApp) "System-App" else "Nutzer-App"
            val permsFormatted =
              app.grantedDangerousPermissions.joinToString(separator = ", ") { perm ->
                "${perm.shortName} [${perm.category.label}]"
              }
            appendLine("- ${app.appName} ($originLabel): $permsFormatted")
          }
          if (omittedCount > 0) {
            appendLine()
            appendLine(
              "Hinweis: $omittedCount weitere App(s) mit geringerer Berechtigungsdichte wurden aus Platzgründen ausgelassen."
            )
          }
          appendLine()
          appendLine(
            "Erstelle eine verständliche Zusammenfassung, nenne die wichtigsten Risiken und " +
              "gib konkrete, priorisierte Vorschläge (HIGH, MEDIUM, LOW) inklusive betroffener App-Namen aus obiger Liste."
          )
        }
      }

      return PermissionDigestPayload(
        promptText = promptText,
        analyzedAppCount = selectedApps.size,
        analyzedPermissionCount = permissionCount,
        omittedDangerousAppCount = omittedCount,
      )
    }

    fun parsePrivacyAnalysisJson(
      jsonString: String,
      analyzedAppCount: Int,
      analyzedPermissionCount: Int,
      omittedDangerousAppCount: Int = 0,
      generatedAtMillis: Long = System.currentTimeMillis(),
    ): PrivacyAnalysisReport {
      val root = JSONObject(jsonString)
      val summary = root.optString("summary", "").trim()
      if (summary.isEmpty()) {
        throw IllegalStateException("Modellantwort enthält keine Zusammenfassung.")
      }

      val keyRisksArray = root.optJSONArray("keyRisks")
      val keyRisks =
        if (keyRisksArray != null) {
          (0 until keyRisksArray.length())
            .mapNotNull { idx -> keyRisksArray.optString(idx, "").trim().takeIf { it.isNotEmpty() } }
        } else {
          emptyList()
        }

      val suggestionsArray = root.optJSONArray("suggestions")
      val suggestions =
        if (suggestionsArray != null) {
          (0 until suggestionsArray.length()).mapNotNull { idx ->
            val obj = suggestionsArray.optJSONObject(idx) ?: return@mapNotNull null
            val title = obj.optString("title", "").trim()
            val recommendation = obj.optString("recommendation", "").trim()
            if (title.isEmpty() || recommendation.isEmpty()) return@mapNotNull null
            val rawPriority = obj.optString("priority", "MEDIUM").trim().uppercase()
            val priority =
              PrivacyRecommendationPriority.entries.firstOrNull { it.name == rawPriority }
                ?: PrivacyRecommendationPriority.MEDIUM
            val targetPermission =
              if (obj.isNull("targetPermission")) null
              else
                obj.optString("targetPermission", "").trim().takeIf {
                  it.isNotEmpty() && !it.equals("null", ignoreCase = true)
                }
            val appsArray = obj.optJSONArray("affectedApps")
            val affectedApps =
              if (appsArray != null) {
                (0 until appsArray.length()).mapNotNull { appIdx ->
                  appsArray.optString(appIdx, "").trim().takeIf { it.isNotEmpty() }
                }
              } else {
                emptyList()
              }
            PrivacySuggestion(
              title = title,
              priority = priority,
              recommendation = recommendation,
              targetPermission = targetPermission,
              affectedApps = affectedApps,
            )
          }
        } else {
          emptyList()
        }

      val priorityOrder =
        mapOf(
          PrivacyRecommendationPriority.HIGH to 0,
          PrivacyRecommendationPriority.MEDIUM to 1,
          PrivacyRecommendationPriority.LOW to 2,
        )

      return PrivacyAnalysisReport(
        summary = summary,
        keyRisks = keyRisks,
        suggestions = suggestions.sortedBy { priorityOrder[it.priority] ?: 1 },
        analyzedAppCount = analyzedAppCount,
        analyzedPermissionCount = analyzedPermissionCount,
        omittedDangerousAppCount = omittedDangerousAppCount,
        generatedAtMillis = generatedAtMillis,
      )
    }
  }
}
