package com.example.core.evidence

/**
 * REFERENCE ORACLE ONLY / KEIN MALWARE-DETEKTOR (Issue #13 / SYN-001 bis SYN-009).
 * Läuft ausschließlich mit synthetischen QA-Referenzfällen zur Verifikation der
 * Fail-Closed-Sicherheitsgates. Keine Geräteprüfung, kein Produktiv-Score.
 */
object SyntheticFixtureOracle {
  const val SCHEMA = "devicecare-synthetic-qa-fixtures/1"

  enum class Gate(val label: String) {
    NO_MALWARE_EVIDENCE("NO_MALWARE_EVIDENCE"),
    REVIEW_ONLY("REVIEW_ONLY"),
    SIMULATION_REVIEW("SIMULATION_REVIEW"),
    NOT_ASSESSABLE("NOT_ASSESSABLE"),
  }

  data class Fixture(
    val id: String,
    val title: String,
    val expected: Gate,
    val permissions: List<String> = emptyList(),
    val events: Map<String, Int> = emptyMap(),
    val coverage: String = "full_test_fixture",
    val evidenceFresh: Boolean = true,
    val schema: String = SCHEMA,
  )

  private val allowedPermissions =
    setOf(
      "android.permission.CAMERA",
      "android.permission.READ_SMS",
      "android.permission.POST_NOTIFICATIONS",
    )

  private val allowedEvents =
    setOf(
      "synthetic_abnormal_access_count",
      "synthetic_rename_count",
      "synthetic_encryption_hint",
      "synthetic_benign_backup_count",
      "synthetic_normal_background_count",
    )

  fun assessFixture(f: Fixture): Gate {
    if (
      f.schema != SCHEMA ||
        !f.evidenceFresh ||
        f.coverage !in setOf("full_test_fixture", "known_subset")
    ) {
      return Gate.NOT_ASSESSABLE
    }
    if (
      f.permissions.any { it !in allowedPermissions } ||
        f.events.any { (kind, count) -> kind !in allowedEvents || count !in 0..250 }
    ) {
      return Gate.NOT_ASSESSABLE
    }
    if (f.coverage == "known_subset") {
      return if (f.permissions.isNotEmpty()) Gate.REVIEW_ONLY else Gate.NOT_ASSESSABLE
    }
    if (
      (f.events["synthetic_rename_count"] ?: 0) > 0 &&
        (f.events["synthetic_encryption_hint"] ?: 0) > 0
    ) {
      return Gate.SIMULATION_REVIEW
    }
    if (f.permissions.isNotEmpty()) return Gate.REVIEW_ONLY
    if (
      f.events.isEmpty() || (f.events.keys == setOf("synthetic_benign_backup_count"))
    ) {
      return Gate.NO_MALWARE_EVIDENCE
    }
    return Gate.REVIEW_ONLY
  }

  val referenceCases: List<Fixture> =
    listOf(
      Fixture(
        id = "SYN-001",
        title = "Normale CAMERA-Berechtigung bei eingeschränkter Sichtbarkeit",
        expected = Gate.REVIEW_ONLY,
        permissions = listOf("android.permission.CAMERA"),
        coverage = "known_subset",
      ),
      Fixture(
        id = "SYN-002",
        title = "Vollständig spezifiziertes künstliches Sample ohne Signale",
        expected = Gate.NO_MALWARE_EVIDENCE,
      ),
      Fixture(
        id = "SYN-003",
        title = "Synthetisches READ_SMS + auffälliger Zugriffszähler",
        expected = Gate.REVIEW_ONLY,
        permissions = listOf("android.permission.READ_SMS"),
        events = mapOf("synthetic_abnormal_access_count" to 3),
      ),
      Fixture(
        id = "SYN-004",
        title = "Künstliche Umbenennungs- & Verschlüsselungs-Hinweise",
        expected = Gate.SIMULATION_REVIEW,
        events = mapOf("synthetic_rename_count" to 25, "synthetic_encryption_hint" to 1),
      ),
      Fixture(
        id = "SYN-005",
        title = "Legitimer Backup-Lauf (benign burst)",
        expected = Gate.NO_MALWARE_EVIDENCE,
        events = mapOf("synthetic_benign_backup_count" to 50),
      ),
      Fixture(
        id = "SYN-006",
        title = "Abgelaufenes Sample (stale > 60s)",
        expected = Gate.NOT_ASSESSABLE,
        permissions = listOf("android.permission.CAMERA"),
        evidenceFresh = false,
      ),
      Fixture(
        id = "SYN-007",
        title = "Unbekannte Paket-Sichtbarkeit",
        expected = Gate.NOT_ASSESSABLE,
        coverage = "unknown",
      ),
      Fixture(
        id = "SYN-008",
        title = "Gesperrter Private Space",
        expected = Gate.NOT_ASSESSABLE,
        coverage = "private_space_unavailable",
      ),
      Fixture(
        id = "SYN-009",
        title = "Benachrichtigungs-Berechtigung & normaler Hintergrundzähler",
        expected = Gate.REVIEW_ONLY,
        permissions = listOf("android.permission.POST_NOTIFICATIONS"),
        events = mapOf("synthetic_normal_background_count" to 7),
      ),
    )
}
