package com.example.ai

import com.example.devicecare.PatchAgeSignal
import com.example.devicecare.PrivacySummaryReport
import com.example.features.battery.BatteryDashboardSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val NEXT_STEPS_MODEL_NAME = "gemini-3.8-flash"
private const val NEXT_STEPS_TIMEOUT_MILLIS = 60_000L

enum class NextStepPriority(val label: String) {
  CRITICAL("KRITISCH"),
  HIGH("HOCH"),
  MEDIUM("MITTEL"),
  LOW("NIEDRIG"),
}

enum class NextStepActionHint(val buttonLabel: String?) {
  OPEN_APP_PERMISSIONS("Apps & Rechte öffnen"),
  OPEN_SECURITY_SETTINGS("Sicherheitseinstellungen"),
  OPEN_PLAY_STORE("Play Store öffnen"),
  OPEN_SYSTEM_SECURITY("System-Check öffnen"),
  RUN_LOCAL_CHECKS("Komplett-Check starten"),
  NONE(null),
}

data class PostureNextStep(
  val rank: Int,
  val title: String,
  val priority: NextStepPriority,
  val plainLanguageSummary: String,
  val estimatedEffort: String,
  val actionHint: NextStepActionHint = NextStepActionHint.NONE,
)

data class PostureNextStepsReport(
  val headline: String,
  val postureOverview: String,
  val nextSteps: List<PostureNextStep>,
  val generatedAtMillis: Long = System.currentTimeMillis(),
)

sealed interface PostureNextStepsState {
  data object Idle : PostureNextStepsState

  data object Loading : PostureNextStepsState

  data class Success(val report: PostureNextStepsReport) : PostureNextStepsState

  data class Error(val message: String) : PostureNextStepsState
}

interface PostureNextStepsService {
  suspend fun analyzeDevicePosture(
    summaryReport: PrivacySummaryReport,
    batterySnapshot: BatteryDashboardSnapshot? = null,
  ): PostureNextStepsReport
}

class GeminiPostureNextStepsService : PostureNextStepsService {

  private val nextStepSchema =
    Schema.obj(
      properties =
        mapOf(
          "title" to
            Schema.string(
              description = "Kurzer, klarer Aktionstitel in Alltagssprache (max. 60 Zeichen)"
            ),
          "priority" to
            Schema.enumeration(
              values = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW"),
              description = "Priorität basierend auf Auswirkung auf Privatsphäre und Gerätesicherheit",
            ),
          "plainLanguageSummary" to
            Schema.string(
              description =
                "Verständliche Erklärung ohne Fachjargon in 2 bis 3 Sätzen, was konkret zu tun ist und warum es hilft"
            ),
          "estimatedEffort" to
            Schema.string(
              description = "Kurze Aufwandsschätzung wie '~1 Min.' oder '2–3 Min.'"
            ),
          "actionHint" to
            Schema.enumeration(
              values =
                listOf(
                  "OPEN_APP_PERMISSIONS",
                  "OPEN_SECURITY_SETTINGS",
                  "OPEN_PLAY_STORE",
                  "OPEN_SYSTEM_SECURITY",
                  "RUN_LOCAL_CHECKS",
                  "NONE",
                ),
              description = "Passende App- oder Systemaktion für diesen Schritt",
            ),
        ),
      optionalProperties = listOf("estimatedEffort", "actionHint"),
    )

  private val responseSchemaObj =
    Schema.obj(
      properties =
        mapOf(
          "headline" to
            Schema.string(
              description = "Kurze Überschrift zur aktuellen Sicherheits- und Datenschutzlage"
            ),
          "postureOverview" to
            Schema.string(
              description =
                "Klar formulierte Gesamteinordnung des aktuellen Gerätestatus in einfacher Alltagssprache (2 bis 3 Sätze)"
            ),
          "nextSteps" to
            Schema.array(
              items = nextStepSchema,
              description =
                "Priorisierte Liste von 3 bis 6 konkreten 'Nächsten Schritten' zur Verbesserung von Datenschutz und Sicherheit",
            ),
        )
    )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = NEXT_STEPS_MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = responseSchemaObj
        },
      systemInstruction =
        content {
          text(
            "Du bist der Datenschutz- und Sicherheitsberater im Dashboard 'Datenschutz-Cockpit' von AI-DeviceCare. " +
              "Du erhältst eine anonymisierte Zusammenfassung des aktuellen lokalen Gerätestatus (ohne App-Paketnamen oder persönliche Daten). " +
              "Erstelle auf Deutsch eine priorisierte, leicht verständliche Liste konkreter 'Nächster Schritte' in klarer Alltagssprache. " +
              "Regeln: Erfinde niemals nicht gemessene Fakten oder Risiko-Scores. Wenn Scans fehlen oder externe Dienste " +
              "(wie Play Protect oder Hersteller-Updates) als NOT_PROVEN markiert sind, benenne dies ehrlich und empfehle die passende manuelle Prüfung. " +
              "Schlage keine Root-, ADB- oder unsicheren Drittanbieter-Eingriffe vor."
          )
        },
    )
  }

  override suspend fun analyzeDevicePosture(
    summaryReport: PrivacySummaryReport,
    batterySnapshot: BatteryDashboardSnapshot?,
  ): PostureNextStepsReport {
    val prompt = buildDeviceStatePrompt(summaryReport, batterySnapshot)
    val response = withTimeout(NEXT_STEPS_TIMEOUT_MILLIS) { model.generateContent(prompt) }
    val jsonString =
      response.text?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Gemini hat keine Nächsten-Schritte-Auswertung geliefert.")
    return parseNextStepsJson(jsonString)
  }

  companion object {
    /**
     * Builds an anonymized, aggregate-only summary of the current local device posture
     * (never includes raw package names, device identifiers, or user account details).
     */
    fun buildDeviceStatePrompt(
      report: PrivacySummaryReport,
      batterySnapshot: BatteryDashboardSnapshot? = null,
    ): String = buildString {
      appendLine("Bewerte den folgenden aktuellen, anonymisierten Gerätestatus für das Datenschutz-Cockpit:")
      appendLine("- Gesamteinstufung lokal: ${report.postureTier.title} (${report.postureTier.badge})")
      appendLine("- Abgeschlossene lokale Prüfmodule: ${report.checksCompletedCount} von ${report.checksTotalCount}")
      appendLine(
        "- Berechtigungsscan durchgeführt: ${if (report.hasPermissionScan) "ja" else "nein (NOT_PROVEN)"}"
      )
      if (report.hasPermissionScan) {
        appendLine("  * Sichtbare Apps insgesamt: ${report.visibleAppsCount}")
        appendLine("  * Apps mit gefährlichen Laufzeitrechten: ${report.appsWithDangerousCount}")
        appendLine("  * Apps mit hochsensiblen Rechten: ${report.appsWithHighSensitivityCount}")
        appendLine("  * Apps mit kritischen Sensoren (Kamera/Standort/Mikrofon): ${report.appsWithCriticalSensorsCount}")
        appendLine("  * Mehrfach kritische Apps (>=2 Sensoren kombiniert): ${report.multiCriticalApps.size}")
        report.sensorMetrics.forEach { metric ->
          appendLine("  * Sensor-Exposition ${metric.label}: ${metric.appCount} App(s)")
        }
      }
      appendLine(
        "- System- & Google-Check durchgeführt: ${if (report.hasSecurityCheck) "ja" else "nein (NOT_PROVEN)"}"
      )
      if (report.hasSecurityCheck) {
        appendLine("  * Betriebssystem: ${report.androidReleaseLabel ?: "NOT_PROVEN"}")
        appendLine("  * Sicherheitspatch: ${report.securityPatch ?: "nicht verfügbar"} (${report.securitySnapshotStatusLabel})")
        appendLine(
          "  * Patch-Alter-Signal: " +
            when (report.patchAgeSignal) {
              PatchAgeSignal.REVIEW_RECOMMENDED -> "REVIEW_RECOMMENDED (>= 60 Tage alt laut Geräteuhr)"
              PatchAgeSignal.NO_AGE_WARNING -> "NO_AGE_WARNING (kein Alterswarnsignal)"
              PatchAgeSignal.NOT_PROVEN -> "NOT_PROVEN"
            }
        )
        appendLine("  * Google Play-Dienste auslesbar: ${if (report.playServicesVersion != null) "ja (${report.playServicesVersion})" else "NOT_PROVEN"}")
        appendLine("  * Google Play Store auslesbar: ${if (report.playStoreVersion != null) "ja (${report.playStoreVersion})" else "NOT_PROVEN"}")
        appendLine("  * WebView-Provider auslesbar: ${if (report.webViewVersion != null) "ja (${report.webViewVersion})" else "NOT_PROVEN"}")
      }
      appendLine("- Lokale Geräteinspektion durchgeführt: ${if (report.hasInspectionRun) "ja" else "nein"}")
      if (batterySnapshot != null && batterySnapshot.errorMessage == null) {
        appendLine(
          "- Zusatzkontext Akku (anonymisiert): Ladestand=${batterySnapshot.percent?.let { "$it%" } ?: "N/A"}, " +
            "Sparmodus=${batterySnapshot.powerSaveMode?.toString() ?: "unbekannt"}"
        )
      }
      appendLine()
      appendLine(
        "Erstelle eine knappe Gesamteinordnung in Alltagssprache und 3 bis 6 priorisierte, " +
          "konkrete 'Nächste Schritte' mit Aufwandsschätzung und passendem actionHint."
      )
    }

    fun parseNextStepsJson(
      jsonString: String,
      generatedAtMillis: Long = System.currentTimeMillis(),
    ): PostureNextStepsReport {
      val root = JSONObject(jsonString)
      val headline = root.optString("headline", "").trim()
      val overview = root.optString("postureOverview", "").trim()
      if (headline.isEmpty() || overview.isEmpty()) {
        throw IllegalStateException("Modellantwort enthält keine gültige Gesamteinordnung.")
      }

      val stepsArray = root.optJSONArray("nextSteps")
      val rawSteps =
        if (stepsArray != null) {
          (0 until stepsArray.length()).mapNotNull { idx ->
            val obj = stepsArray.optJSONObject(idx) ?: return@mapNotNull null
            val title = obj.optString("title", "").trim()
            val summary = obj.optString("plainLanguageSummary", "").trim()
            if (title.isEmpty() || summary.isEmpty()) return@mapNotNull null

            val rawPriority = obj.optString("priority", "MEDIUM").trim().uppercase()
            val priority =
              NextStepPriority.entries.firstOrNull { it.name == rawPriority }
                ?: NextStepPriority.MEDIUM

            val effort =
              obj.optString("estimatedEffort", "").trim().ifEmpty { "~2 Min." }

            val rawHint = obj.optString("actionHint", "NONE").trim().uppercase()
            val actionHint =
              NextStepActionHint.entries.firstOrNull { it.name == rawHint }
                ?: NextStepActionHint.NONE

            PostureNextStep(
              rank = idx + 1,
              title = title,
              priority = priority,
              plainLanguageSummary = summary,
              estimatedEffort = effort,
              actionHint = actionHint,
            )
          }
        } else {
          emptyList()
        }

      if (rawSteps.isEmpty()) {
        throw IllegalStateException("Modellantwort enthält keine konkreten Nächsten Schritte.")
      }

      val sortedSteps =
        rawSteps
          .sortedBy { it.priority.ordinal }
          .mapIndexed { index, step -> step.copy(rank = index + 1) }

      return PostureNextStepsReport(
        headline = headline,
        postureOverview = overview,
        nextSteps = sortedSteps,
        generatedAtMillis = generatedAtMillis,
      )
    }
  }
}
