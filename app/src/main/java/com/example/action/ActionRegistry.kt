package com.example.action

import com.example.data.model.ActionProposal
import com.example.data.model.RiskLevel

object ActionRegistry {

    const val ACTION_OPEN_APP = "open_app"
    const val ACTION_SEARCH_WEB = "search_web"
    const val ACTION_SET_ALARM = "set_alarm"
    const val ACTION_CREATE_REMINDER = "create_reminder"
    const val ACTION_MAKE_CALL = "make_call"
    const val ACTION_SEND_SMS = "send_sms"
    const val ACTION_SEARCH_CONTACTS = "search_contacts"
    const val ACTION_OPEN_SETTINGS = "open_settings"
    const val ACTION_TOGGLE_TORCH = "toggle_torch"
    const val ACTION_OPEN_CAMERA = "open_camera"
    const val ACTION_VIEW_CALENDAR = "view_calendar"
    const val ACTION_RUN_WORKFLOW = "run_workflow"

    data class ActionDescriptor(
        val actionId: String,
        val displayName: String,
        val description: String,
        val defaultRisk: RiskLevel,
        val requiredParameters: List<String>,
        val requiresConfirmation: Boolean
    )

    private val supportedActions = mapOf(
        ACTION_OPEN_APP to ActionDescriptor(
            actionId = ACTION_OPEN_APP,
            displayName = "Open Application",
            description = "Launch an installed application on device",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("app_name"),
            requiresConfirmation = false
        ),
        ACTION_SEARCH_WEB to ActionDescriptor(
            actionId = ACTION_SEARCH_WEB,
            displayName = "Search Web",
            description = "Search Google or web for information",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("query"),
            requiresConfirmation = false
        ),
        ACTION_SET_ALARM to ActionDescriptor(
            actionId = ACTION_SET_ALARM,
            displayName = "Set Alarm",
            description = "Schedule a system alarm",
            defaultRisk = RiskLevel.MEDIUM,
            requiredParameters = listOf("hour", "minutes"),
            requiresConfirmation = true
        ),
        ACTION_CREATE_REMINDER to ActionDescriptor(
            actionId = ACTION_CREATE_REMINDER,
            displayName = "Create Reminder",
            description = "Save a local or calendar reminder",
            defaultRisk = RiskLevel.MEDIUM,
            requiredParameters = listOf("title"),
            requiresConfirmation = false
        ),
        ACTION_MAKE_CALL to ActionDescriptor(
            actionId = ACTION_MAKE_CALL,
            displayName = "Prepare Phone Call",
            description = "Open phone dialer to call a specified contact",
            defaultRisk = RiskLevel.MEDIUM,
            requiredParameters = listOf("contact_or_number"),
            requiresConfirmation = true
        ),
        ACTION_SEND_SMS to ActionDescriptor(
            actionId = ACTION_SEND_SMS,
            displayName = "Draft SMS Message",
            description = "Compose an SMS to review and send",
            defaultRisk = RiskLevel.MEDIUM,
            requiredParameters = listOf("recipient", "message"),
            requiresConfirmation = true
        ),
        ACTION_SEARCH_CONTACTS to ActionDescriptor(
            actionId = ACTION_SEARCH_CONTACTS,
            displayName = "Search Contacts",
            description = "Search device address book",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("query"),
            requiresConfirmation = false
        ),
        ACTION_OPEN_SETTINGS to ActionDescriptor(
            actionId = ACTION_OPEN_SETTINGS,
            displayName = "Open Settings",
            description = "Navigate directly to system settings (Wi-Fi, Bluetooth, etc.)",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("setting"),
            requiresConfirmation = false
        ),
        ACTION_TOGGLE_TORCH to ActionDescriptor(
            actionId = ACTION_TOGGLE_TORCH,
            displayName = "Toggle Flashlight",
            description = "Turn device torch on or off",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("state"),
            requiresConfirmation = false
        ),
        ACTION_OPEN_CAMERA to ActionDescriptor(
            actionId = ACTION_OPEN_CAMERA,
            displayName = "Open Camera",
            description = "Launch camera viewfinder",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = emptyList(),
            requiresConfirmation = false
        ),
        ACTION_VIEW_CALENDAR to ActionDescriptor(
            actionId = ACTION_VIEW_CALENDAR,
            displayName = "Open Calendar",
            description = "View device calendar schedule",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = emptyList(),
            requiresConfirmation = false
        ),
        ACTION_RUN_WORKFLOW to ActionDescriptor(
            actionId = ACTION_RUN_WORKFLOW,
            displayName = "Execute Automation Routine",
            description = "Execute multi-step automation workflow",
            defaultRisk = RiskLevel.LOW,
            requiredParameters = listOf("workflow_name"),
            requiresConfirmation = false
        )
    )

    fun getDescriptor(actionId: String): ActionDescriptor? = supportedActions[actionId]

    fun getAllDescriptors(): List<ActionDescriptor> = supportedActions.values.toList()

    /**
     * Validates an action proposal against registry schema.
     * Prevents arbitrary or unauthorized execution.
     */
    fun validateProposal(proposal: ActionProposal): ActionProposal? {
        val descriptor = supportedActions[proposal.actionId] ?: return null
        for (param in descriptor.requiredParameters) {
            if (!proposal.parameters.containsKey(param) || proposal.parameters[param].isNullOrBlank()) {
                return null
            }
        }
        return proposal.copy(
            riskLevel = descriptor.defaultRisk,
            requiresConfirmation = descriptor.requiresConfirmation
        )
    }
}
