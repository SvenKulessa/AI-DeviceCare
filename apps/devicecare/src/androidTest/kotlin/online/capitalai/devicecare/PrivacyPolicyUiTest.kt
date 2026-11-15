package online.capitalai.devicecare

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** UI-only checks: not a legal approval or proof of on-device network behavior. */
class PrivacyPolicyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun privacyAvailableBeforeInspectionAndBackNavigationWorks() {
        rule.onNodeWithTag("privacy_policy_open").performScrollTo().performClick()
        rule.onNodeWithTag("privacy_policy_screen").assertExists()
        rule.onNodeWithTag("privacy_policy_title").assertExists()
        rule.onNodeWithTag("privacy_policy_access").assertExists()
        rule.onNodeWithTag("privacy_policy_network").assertExists()
        // Scanning must not be possible from the standalone policy view.
        rule.onNodeWithTag("inspection_start_button").assertDoesNotExist()
        rule.onNodeWithTag("privacy_policy_back").performScrollTo().performClick()
        rule.onNodeWithTag("inspection_start_button").performScrollTo().assertExists()
        // Privacy stays reachable, and there is still no persisted inspection.
        rule.onNodeWithTag("privacy_policy_open").performScrollTo().assertExists()
    }
}
