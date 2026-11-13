package online.capitalai.devicecare

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** On emulator this confirms interaction only, not Google/OEM update availability. */
class SystemSecurityWorkflowUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun independentSecurityScreenRequiresManualCollectionAndReturnsToHome() {
        rule.onNodeWithTag("system_security_open").performScrollTo().performClick()
        rule.onNodeWithTag("system_security_workflow").assertExists()
        rule.onNodeWithText("Noch keine Prüfung ausgeführt (NOT_PROVEN).").assertExists()
        rule.onNodeWithTag("system_security_start")
            .performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Google Play-Systempatch:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Play Protect: NOT_PROVEN", substring = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("system_security_back").performScrollTo().performClick()
        rule.onNodeWithTag("inspection_start_button").performScrollTo().assertExists()
    }
}
