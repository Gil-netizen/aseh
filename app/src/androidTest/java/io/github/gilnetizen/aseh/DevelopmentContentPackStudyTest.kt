package io.github.gilnetizen.aseh

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevelopmentContentPackStudyTest {
    private val activityRule = ActivityScenarioRule<MainActivity>(
        Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java,
        ).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        },
    )

    @get:Rule(order = 0)
    val locationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION)

    @get:Rule(order = 1)
    val composeRule = AndroidComposeTestRule(activityRule) { rule ->
        lateinit var activity: MainActivity
        rule.scenario.onActivity { activity = it }
        activity
    }

    @Test
    fun developmentPackIsVerifiedAndSearchableOrFailsClosedWhenCryptoIsUnavailable() {
        composeRule.onNodeWithTag("destination-study").performClick()
        composeRule.onNodeWithTag("study-development-pack").assertIsDisplayed()

        if (Build.VERSION.SDK_INT == 26) {
            waitForTag("study-development-pack-unsupported")
            composeRule.onNodeWithTag("study-development-pack-unsupported").assertIsDisplayed()
            assertNoNode("study-development-pack-ready")
            return
        }

        waitForTag("study-development-pack-ready")
        composeRule.onNodeWithTag("study-development-pack-ready").assertIsDisplayed()
        waitForTag("study-development-pack-hit-synthetic.guide.rehearsal")
        val rehearsalResult = composeRule.onNodeWithTag(
            "study-development-pack-hit-synthetic.guide.rehearsal",
        )
        rehearsalResult
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        assertEquals(
            Role.Button,
            rehearsalResult.fetchSemanticsNode().config[SemanticsProperties.Role],
        )
        rehearsalResult.performClick()
        composeRule.onNodeWithTag("study-development-pack-detail-synthetic.guide.rehearsal")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("study-development-pack-detail-heading")
            .assertTextContains("Synthetic role rehearsal")
        composeRule.onNodeWithTag("study-development-pack-detail-id")
            .assertTextContains("synthetic.guide.rehearsal")
        composeRule.onNodeWithTag("study-development-pack-detail-text")
            .assertTextContains("invented rehearsal roles", substring = true)
        composeRule.onNodeWithTag("study-development-pack-detail-back")
            .performScrollTo()
            .performClick()
        waitForTag("study-development-pack-hit-synthetic.guide.rehearsal")

        composeRule.onNodeWithText("תפקידים", substring = true)
            .performScrollTo()
            .performClick()
        waitForTag("study-development-pack-hit-synthetic.source.roles.he")
        composeRule.onNodeWithTag("study-development-pack-hit-synthetic.source.roles.he")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertNoNode(tag: String) {
        check(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) {
            "Expected no node with tag $tag"
        }
    }
}
