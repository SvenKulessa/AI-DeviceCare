package online.capitalai.devicecare

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Android 15 emulator smoke tests, not OEM accuracy, device power or TalkBack
 * certification. The only execution path into collectors is the user's click.
 */
class M1InspectionAccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun hasExplicitAccessibleScanButtonAndNoAutomaticInspection() {
        rule.onNodeWithTag("inspection_start_button")
            .performScrollTo().assertExists().assertIsDisplayed().assertIsEnabled().assertHasClickAction()
        rule.onNodeWithText("Risk score: NOT_ASSESSABLE", substring = true).assertExists()
        rule.onNodeWithText("Keine Speicherung, kein Upload", substring = true).assertExists()
        rule.onNodeWithText("Momentaufnahme;", substring = true).assertExists()
        assertTrue(rule.onAllNodesWithText("Quelle:", substring = true)
            .fetchSemanticsNodes().isEmpty())
    }

    @Test fun userClickMakesOnlyScopedLocalObservationsVisible() {
        rule.onNodeWithTag("inspection_start_button").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Quelle:", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            rule.onAllNodesWithText("Lokale Prüfung nicht verfügbar", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Risk score: NOT_ASSESSABLE", substring = true).assertExists()
        // The test does not infer mobile-device energy consumption or malware status.
    }
}
