package com.example.data.repository

import com.example.data.model.AssistantLanguage
import java.util.regex.Pattern

data class LanguageDetectionResult(
    val language: AssistantLanguage,
    val languageCode: String,
    val confidence: Float,
    val geminiDirective: String,
    val ttsLocale: String
)

/**
 * Dedicated logic layer for analyzing user input language (English, Telugu, Hindi, or Tanglish)
 * and tailoring prompt directives for the Gemini API before request dispatch.
 */
class LanguageDetector {

    private val teluguScriptPattern = Pattern.compile("[\\u0C00-\\u0C7F]")
    private val devanagariScriptPattern = Pattern.compile("[\\u0900-\\u097F]")

    // Comprehensive phonetic Tanglish (Telugu written in Roman script) dictionary
    private val tanglishTokens = setOf(
        // Verbs & Imperatives
        "cheyi", "chey", "cheyyi", "cheyyandi", "pettuko", "pettu", "pettandi",
        "cheppu", "cheppandi", "chudu", "chudandi", "ivvu", "ivvandi", "ravali",
        "vellu", "vellandi", "vachey", "chusi", "matladu", "matladandi", "kavala",
        "avvaledhu", "avuthundhi", "undhi", "undha", "chesava", "chesara",
        "chusesa", "chesanu", "pettanu", "open cheyi", "close cheyi", "set cheyi",
        // Interrogatives & Pronouns
        "ekkada", "ela", "eppudu", "endhuku", "enti", "emiti", "evvaru", "evaru",
        "nenu", "naaku", "meeku", "manaki", "meeru", "nuvvu", "vaallu", "veellu",
        // Common conversation, greetings & temporal
        "namaskaram", "dhanyavadhalu", "bagunnava", "bagunnara", "repati", "eroju",
        "udayam", "ratri", "sayantram", "samayam", "koddiga", "chala", "chaala",
        "ippudu", "malli", "kuda", "kooda", "alage", "alaage", "sare", "avunu",
        "kaadhu", "ledu", "ledhu"
    )

    // Common Hinglish words (Romanized Hindi)
    private val hinglishTokens = setOf(
        "karo", "kijiye", "batao", "bataiye", "kya", "kaise", "kahan", "kab", "kyun",
        "hoga", "nahi", "nahin", "namaste", "dhanyawad", "shukriya", "aaj", "kal",
        "abhi", "suno", "sunao", "chalao", "kholo", "lagao"
    )

    /**
     * Detects user input language and generates customized Gemini instructions.
     */
    fun analyze(query: String, preferredOverride: AssistantLanguage = AssistantLanguage.AUTO): LanguageDetectionResult {
        // If user explicitly locked a specific language (not AUTO), respect their choice while analyzing
        val detected = if (preferredOverride != AssistantLanguage.AUTO) {
            preferredOverride
        } else {
            classifyLanguage(query)
        }

        val directive = buildGeminiDirective(detected)
        val ttsLocale = when (detected) {
            AssistantLanguage.TELUGU -> "te-IN"
            AssistantLanguage.HINDI -> "hi-IN"
            AssistantLanguage.TANGLISH -> "te-IN"
            AssistantLanguage.ENGLISH, AssistantLanguage.AUTO -> "en-US"
        }

        val confidence = when (detected) {
            AssistantLanguage.TELUGU -> if (teluguScriptPattern.matcher(query).find()) 0.98f else 0.85f
            AssistantLanguage.HINDI -> if (devanagariScriptPattern.matcher(query).find()) 0.98f else 0.85f
            AssistantLanguage.TANGLISH -> 0.90f
            AssistantLanguage.ENGLISH -> 0.95f
            AssistantLanguage.AUTO -> 0.90f
        }

        return LanguageDetectionResult(
            language = detected,
            languageCode = detected.code,
            confidence = confidence,
            geminiDirective = directive,
            ttsLocale = ttsLocale
        )
    }

    private fun classifyLanguage(text: String): AssistantLanguage {
        // 1. Script checks
        if (teluguScriptPattern.matcher(text).find()) {
            return AssistantLanguage.TELUGU
        }
        if (devanagariScriptPattern.matcher(text).find()) {
            return AssistantLanguage.HINDI
        }

        // 2. Tokenized phonetic checks for Romanized Indian languages (Tanglish / Hinglish)
        val lowerText = text.lowercase()
        val words = lowerText.split(Regex("[\\s,?.!;:\"'()]+")).filter { it.isNotBlank() }

        var tanglishScore = 0
        var hinglishScore = 0

        for (word in words) {
            if (tanglishTokens.contains(word)) {
                tanglishScore += 2
            } else if (tanglishTokens.any { token -> word.endsWith(token) || word.startsWith(token) }) {
                tanglishScore += 1
            }

            if (hinglishTokens.contains(word)) {
                hinglishScore += 2
            }
        }

        // Tanglish subphrase checks (e.g., "open cheyi", "alarm pettu")
        if (lowerText.contains(" cheyi") || lowerText.contains(" chey") || lowerText.contains(" pettu") ||
            lowerText.contains(" cheppu") || lowerText.contains(" ekkada") || lowerText.contains(" ela undhi")) {
            tanglishScore += 3
        }

        if (tanglishScore >= 2 && tanglishScore >= hinglishScore) {
            return AssistantLanguage.TANGLISH
        }

        if (hinglishScore >= 2) {
            return AssistantLanguage.HINDI
        }

        return AssistantLanguage.ENGLISH
    }

    private fun buildGeminiDirective(language: AssistantLanguage): String {
        return when (language) {
            AssistantLanguage.TELUGU -> """
                [LANGUAGE DIRECTIVE: TELUGU (తెలుగు)]
                - The user spoke or typed in Telugu.
                - Respond strictly in fluent, natural Telugu using proper Telugu script (తెలుగు లిపి).
                - Use a polite, respectful tone (గౌరవప్రదమైన శైలి, e.g. నమస్కారం, మీరు, చేయగలను).
                - Keep sentences concise, clear, and easy to pronounce via Text-To-Speech.
            """.trimIndent()

            AssistantLanguage.HINDI -> """
                [LANGUAGE DIRECTIVE: HINDI (हिन्दी)]
                - The user spoke or typed in Hindi.
                - Respond in fluent, respectful Hindi using Devanagari script (देवनागरी लिपि).
                - Use a courteous assistant tone (e.g. नमस्ते, आप, मैं आपकी सहायता कर सकता हूँ).
                - Keep sentences concise and natural for Android speech synthesis.
            """.trimIndent()

            AssistantLanguage.TANGLISH -> """
                [LANGUAGE DIRECTIVE: TANGLISH (Telugu-English mixed)]
                - The user is communicating in Tanglish (Telugu words transcribed in English/Roman script).
                - Respond conversationally in natural Tanglish (e.g. "Sure sir, YouTube app ni open chestunnanu", "Alarm set cheyyadaniki ready ga undhi").
                - Maintain the sophisticated JARVIS persona while seamlessly using natural colloquial Telugu phrases in Roman script.
                - Keep responses under 2-3 short sentences.
            """.trimIndent()

            AssistantLanguage.ENGLISH, AssistantLanguage.AUTO -> """
                [LANGUAGE DIRECTIVE: ENGLISH]
                - The user spoke or typed in English.
                - Respond in fluent, professional, articulate English with the distinctive JARVIS personality.
                - Keep responses concise, direct, and under 3 sentences for seamless voice feedback.
            """.trimIndent()
        }
    }
}
