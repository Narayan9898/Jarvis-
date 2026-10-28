package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.ActionRegistry
import com.example.data.model.ActionProposal
import com.example.data.model.RiskLevel
import com.example.data.repository.AiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun `action registry validates supported action`() {
        val proposal = ActionProposal(
            actionId = ActionRegistry.ACTION_OPEN_APP,
            name = "Open YouTube",
            description = "Launch app",
            parameters = mapOf("app_name" to "youtube")
        )
        val validated = ActionRegistry.validateProposal(proposal)
        assertNotNull(validated)
        assertEquals(RiskLevel.LOW, validated?.riskLevel)
        assertEquals(false, validated?.requiresConfirmation)
    }

    @Test
    fun `ai repository detects telugu and tanglish`() {
        val aiRepo = AiRepository()
        assertEquals("te", aiRepo.detectLanguage("జార్విస్ నమస్కారం"))
        assertEquals("hi", aiRepo.detectLanguage("नमस्ते जार्विस"))
        assertEquals("tanglish", aiRepo.detectLanguage("YouTube open cheyi"))
        assertEquals("en", aiRepo.detectLanguage("Hello JARVIS, open YouTube"))
    }

    @Test
    fun `ai repository parses local action intents`() {
        val aiRepo = AiRepository()
        val openAction = aiRepo.extractLocalAction("Open YouTube")
        assertNotNull(openAction)
        assertEquals(ActionRegistry.ACTION_OPEN_APP, openAction?.actionId)

        val alarmAction = aiRepo.extractLocalAction("Set alarm for 7 AM")
        assertNotNull(alarmAction)
        assertEquals(ActionRegistry.ACTION_SET_ALARM, alarmAction?.actionId)
    }
}
