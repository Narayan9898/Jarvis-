package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.ActionRegistry
import com.example.data.model.ActionProposal
import com.example.data.model.RiskLevel
import com.example.data.repository.AiRepository
import com.example.voice.SpeechManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `language detector prepares context aware gemini directives`() {
        val detector = com.example.data.repository.LanguageDetector()

        // 1. Telugu Detection
        val teluguResult = detector.analyze("రేపు ఉదయం 6 గంటలకు అలారం పెట్టు")
        assertEquals(com.example.data.model.AssistantLanguage.TELUGU, teluguResult.language)
        assertEquals("te-IN", teluguResult.ttsLocale)
        assertTrue(teluguResult.geminiDirective.contains("TELUGU"))

        // 2. Hindi Detection
        val hindiResult = detector.analyze("नमस्ते जार्विस, आप कैसे हैं?")
        assertEquals(com.example.data.model.AssistantLanguage.HINDI, hindiResult.language)
        assertEquals("hi-IN", hindiResult.ttsLocale)
        assertTrue(hindiResult.geminiDirective.contains("HINDI"))

        // 3. Tanglish Detection
        val tanglishResult = detector.analyze("Eroju evening Ravi ki message cheyi")
        assertEquals(com.example.data.model.AssistantLanguage.TANGLISH, tanglishResult.language)
        assertEquals("te-IN", tanglishResult.ttsLocale)
        assertTrue(tanglishResult.geminiDirective.contains("TANGLISH"))

        // 4. English Detection
        val englishResult = detector.analyze("What is the weather in London today?")
        assertEquals(com.example.data.model.AssistantLanguage.ENGLISH, englishResult.language)
        assertEquals("en-US", englishResult.ttsLocale)
        assertTrue(englishResult.geminiDirective.contains("ENGLISH"))
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

    @Test
    fun `speech manager continuous listening toggle and lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val speechManager = SpeechManager(context)

        assertFalse(speechManager.isContinuousMode.value)

        speechManager.setContinuousListening(true)
        assertTrue(speechManager.isContinuousMode.value)

        // Test pausing during TTS
        speechManager.pauseForTts()
        assertFalse(speechManager.isListening.value)

        speechManager.resumeAfterTts()
        // Still in continuous mode
        assertTrue(speechManager.isContinuousMode.value)

        speechManager.setContinuousListening(false)
        assertFalse(speechManager.isContinuousMode.value)

        speechManager.destroy()
    }
}
