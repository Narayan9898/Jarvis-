package com.example.data.model

data class ActionProposal(
    val actionId: String,
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresConfirmation: Boolean = false,
    val rawQuery: String = ""
)

data class ActionResult(
    val success: Boolean,
    val message: String,
    val detail: String? = null,
    val openIntentPackage: String? = null
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)
