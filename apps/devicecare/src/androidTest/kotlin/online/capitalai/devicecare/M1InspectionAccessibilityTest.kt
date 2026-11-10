package online.capitalai.devicecare

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.platform.app.InstrumentationRegistry
import online.capitalai.devicecare.features.permissions.AndroidPermissionCollector
import org.junit.Assert.assertEquals
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
        rule.onNodeWithText("Geräteprofil").assertDoesNotExist()
        rule.onNodeWithText("Energiesparmodus öffnen").assertDoesNotExist()
    }

    @Test fun scrollContentStaysOutsideSystemBars() {
        val contentBounds = rule.onNodeWithTag("devicecare_scroll_content")
            .fetchSemanticsNode().boundsInWindow

        rule.runOnIdle {
            val window = rule.activity.window
            val decor = window.decorView
            val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
                .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            assertTrue("Expected visible system insets", insets.top + insets.bottom + insets.left + insets.right > 0)
            assertTrue("Content overlaps the top system inset", contentBounds.top >= insets.top)
            assertTrue("Content overlaps the left system inset", contentBounds.left >= insets.left)
            assertTrue("Content overlaps the right system inset", contentBounds.right <= decor.width - insets.right)
            assertTrue("Content overlaps the bottom system inset", contentBounds.bottom <= decor.height - insets.bottom)

            val bars = WindowInsetsControllerCompat(window, decor)
            assertTrue("Status bar icons should be dark on the light surface", bars.isAppearanceLightStatusBars)
            assertTrue("Navigation bar icons should be dark on the light surface", bars.isAppearanceLightNavigationBars)
        }

        rule.onNodeWithText("Cloud, analytics, VPN", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun reducedCollectorCapsPermissionMetadataSampling() {
        val batch = AndroidPermissionCollector(rule.activity.applicationContext).collect(sampleLimit = 1)
        val values = batch.observations.associate { it.id to it.value }
        assertEquals("1", values["sampled_packages"])
        val visible = checkNotNull(values["visible_packages"]).toInt()
        assertTrue("At least the app itself should be visible", visible >= 1)
        assertEquals((visible > 1).toString(), values["sample_truncated"])
    }

    @Test fun userClickMakesOnlyScopedLocalObservationsVisible() {
        // Verify the running Activity picked up the emulator's requested setting.
        InstrumentationRegistry.getArguments().getString("expectedFontScale")?.let { expected ->
            assertEquals(
                "Requested font scale must be active before checking scan results",
                expected.toFloat(),
                rule.activity.resources.configuration.fontScale,
                0f,
            )
        }
        rule.onNodeWithTag("inspection_start_button").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Quelle:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Risk score: NOT_ASSESSABLE", substring = true).assertExists()
        rule.onNodeWithText("Prüfumfang (Android API", substring = true).performScrollTo().assertIsDisplayed()
        for (label in listOf("Geräteprofil", "Thermische Belastung:", "Akkutemperatur:", "App-Berechtigungen:")) {
            rule.onNodeWithText(label, substring = true).performScrollTo().assertIsDisplayed()
        }
        for (label in listOf("Energiesparmodus öffnen", "App-Info von AI-DeviceCare öffnen")) {
            rule.onNodeWithText(label).performScrollTo().assertIsDisplayed().assertHasClickAction()
        }
        // The test does not infer mobile-device energy consumption or malware status.
    }
}
