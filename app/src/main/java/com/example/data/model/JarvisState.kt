package com.example.data.model

enum class JarvisState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    CONFIRMATION_REQUIRED,
    ERROR
}

enum class AssistantLanguage(val code: String, val displayName: String, val ttsLocale: String) {
    AUTO("auto", "Auto Detect", "en-US"),
    ENGLISH("en", "English", "en-US"),
    TELUGU("te", "తెలుగు (Telugu)", "te-IN"),
    HINDI("hi", "हिन्दी (Hindi)", "hi-IN"),
    TANGLISH("tanglish", "Tanglish (Telugu-English)", "te-IN")
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
