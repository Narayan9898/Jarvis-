package com.example.workflow

import com.example.action.ActionExecutor
import com.example.action.ActionRegistry
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.WorkflowEntity
import com.example.data.model.ActionProposal
import com.example.voice.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkflowEngine(
    private val database: JarvisDatabase,
    private val actionExecutor: ActionExecutor,
    private val ttsManager: TtsManager
) {
    private val _isExecutingWorkflow = MutableStateFlow(false)
    val isExecutingWorkflow: StateFlow<Boolean> = _isExecutingWorkflow.asStateFlow()

    private val _currentStatus = MutableStateFlow("")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    suspend fun executeWorkflow(workflow: WorkflowEntity, onStepUpdate: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        _isExecutingWorkflow.value = true
        _currentStatus.value = "Starting routine: ${workflow.name}"
        onStepUpdate("Initiating routine: ${workflow.name}")

        try {
            val steps = JSONArray(workflow.stepsJson)
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                val type = step.optString("type")

                when (type) {
                    "speak" -> {
                        val text = step.optString("text")
                        _currentStatus.value = text
                        onStepUpdate(text)
                        ttsManager.speak(text)
                        delay(2500)
                    }
                    "date" -> {
                        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                        val now = Date()
                        val msg = "Today is ${dateFormat.format(now)}. Current time is ${timeFormat.format(now)}."
                        _currentStatus.value = msg
                        onStepUpdate(msg)
                        ttsManager.speak(msg)
                        delay(3000)
                    }
                    "reminders" -> {
                        val reminderDao = database.reminderDao()
                        val pending = reminderDao.getAllRemindersFlow()
                        val msg = "Checking pending tasks and reminders for you."
                        _currentStatus.value = msg
                        onStepUpdate(msg)
                        ttsManager.speak(msg)
                        delay(2500)
                    }
                    "action" -> {
                        val actionId = step.optString("actionId")
                        val paramsObj = step.optJSONObject("params")
                        val params = mutableMapOf<String, String>()
                        paramsObj?.keys()?.forEach { k -> params[k] = paramsObj.getString(k) }

                        val proposal = ActionProposal(
                            actionId = actionId,
                            name = step.optString("name", "Workflow Action"),
                            description = "Automated workflow step",
                            parameters = params
                        )
                        val validated = ActionRegistry.validateProposal(proposal)
                        if (validated != null) {
                            val result = actionExecutor.execute(validated)
                            onStepUpdate(result.message)
                        }
                        delay(1500)
                    }
                }
            }

            database.workflowDao().updateLastRun(workflow.id, System.currentTimeMillis())
            _currentStatus.value = "Routine '${workflow.name}' completed."
            onStepUpdate("Routine finished successfully.")
            return@withContext true
        } catch (e: Exception) {
            _currentStatus.value = "Workflow interrupted: ${e.message}"
            onStepUpdate("Routine failed: ${e.message}")
            return@withContext false
        } finally {
            _isExecutingWorkflow.value = false
        }
    }
}
