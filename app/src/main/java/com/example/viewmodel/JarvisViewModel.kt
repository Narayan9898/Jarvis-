package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.JarvisApplication
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.WorkflowEntity
import com.example.data.model.ActionProposal
import com.example.data.model.AppInfo
import com.example.data.model.AssistantLanguage
import com.example.data.model.JarvisState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    APPS,
    REMINDERS,
    WORKFLOWS,
    HISTORY,
    PERMISSIONS,
    MEMORY,
    SETTINGS
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val database = app.database
    private val actionExecutor = app.actionExecutor
    val speechManager = app.speechManager
    private val ttsManager = app.ttsManager
    private val aiRepository = app.aiRepository
    private val workflowEngine = app.workflowEngine

    // State flows
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _jarvisState = MutableStateFlow(JarvisState.IDLE)
    val jarvisState: StateFlow<JarvisState> = _jarvisState.asStateFlow()

    private val _statusText = MutableStateFlow("Hello! I am JARVIS. How can I help you today?")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AssistantLanguage.AUTO)
    val selectedLanguage: StateFlow<AssistantLanguage> = _selectedLanguage.asStateFlow()

    private val _pendingAction = MutableStateFlow<ActionProposal?>(null)
    val pendingAction: StateFlow<ActionProposal?> = _pendingAction.asStateFlow()

    private val _apiKey = MutableStateFlow(
        try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
    )
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isContinuousListening: StateFlow<Boolean> = speechManager.isContinuousMode
    val rmsVolume: StateFlow<Float> = speechManager.rmsVolume
    val partialSpeech: StateFlow<String> = speechManager.partialText
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    val isExecutingWorkflow: StateFlow<Boolean> = workflowEngine.isExecutingWorkflow
    val workflowStatus: StateFlow<String> = workflowEngine.currentStatus

    // Database flows
    val messagesFlow: StateFlow<List<ChatMessageEntity>> = database.conversationDao()
        .getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val remindersFlow: StateFlow<List<ReminderEntity>> = database.reminderDao()
        .getAllRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val workflowsFlow: StateFlow<List<WorkflowEntity>> = database.workflowDao()
        .getAllWorkflowsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val memoryFlow: StateFlow<List<MemoryEntity>> = database.memoryDao()
        .getAllMemoryFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        setupSpeechAndTtsCallbacks()
        loadInstalledApps()
    }

    private fun setupSpeechAndTtsCallbacks() {
        // Speech Recognizer callbacks
        speechManager.onSpeechResult = { text ->
            _jarvisState.value = JarvisState.THINKING
            handleIncomingUserQuery(text)
        }

        speechManager.onPartialSpeech = { partial ->
            if (_jarvisState.value != JarvisState.THINKING && _jarvisState.value != JarvisState.SPEAKING) {
                _jarvisState.value = JarvisState.LISTENING
            }
        }

        speechManager.onSpeechError = { errorMsg ->
            if (!speechManager.isContinuousMode.value) {
                _jarvisState.value = JarvisState.ERROR
                _statusText.value = errorMsg
                viewModelScope.launch {
                    database.conversationDao().insertMessage(
                        ChatMessageEntity(sender = "SYSTEM", text = errorMsg)
                    )
                }
            }
        }

        // TTS Speech lifecycle synchronization: pause recognizer during speech, resume after
        ttsManager.onSpeechStarted = {
            speechManager.pauseForTts()
        }

        ttsManager.onSpeechCompleted = {
            speechManager.resumeAfterTts()
            if (_jarvisState.value == JarvisState.SPEAKING) {
                _jarvisState.value = if (speechManager.isContinuousMode.value) JarvisState.LISTENING else JarvisState.IDLE
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = actionExecutor.getInstalledApplications()
            _installedApps.value = apps
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setLanguage(lang: AssistantLanguage) {
        _selectedLanguage.value = lang
    }

    fun setApiKey(newKey: String) {
        _apiKey.value = newKey
    }

    fun startListening() {
        ttsManager.stop()
        _jarvisState.value = JarvisState.LISTENING
        speechManager.startListening(_selectedLanguage.value.code)
    }

    fun stopListening() {
        speechManager.stopListening()
        _jarvisState.value = JarvisState.IDLE
    }

    fun toggleContinuousListening() {
        val nextMode = !speechManager.isContinuousMode.value
        ttsManager.stop()
        speechManager.setContinuousListening(nextMode, _selectedLanguage.value.code)
        if (nextMode) {
            _jarvisState.value = JarvisState.LISTENING
            _statusText.value = "Continuous hands-free listening enabled. Speak any command."
        } else {
            _jarvisState.value = JarvisState.IDLE
            _statusText.value = "Continuous listening paused. Push to talk ready."
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        if (_jarvisState.value == JarvisState.SPEAKING) {
            _jarvisState.value = if (speechManager.isContinuousMode.value) JarvisState.LISTENING else JarvisState.IDLE
        }
    }

    fun sendTextQuery(text: String) {
        if (text.isBlank()) return
        ttsManager.stop()
        _jarvisState.value = JarvisState.THINKING
        handleIncomingUserQuery(text)
    }

    private fun handleIncomingUserQuery(query: String) {
        viewModelScope.launch {
            // Save User message
            database.conversationDao().insertMessage(
                ChatMessageEntity(
                    sender = "USER",
                    text = query,
                    language = _selectedLanguage.value.code
                )
            )

            // Gather memory context & recent conversation turns for Gemini API
            val memories = database.memoryDao().getMemoryByCategory("PREFERENCE")
            val memoryContext = memories.joinToString("; ") { "${it.key}: ${it.value}" }

            val recentEntities = database.conversationDao().getRecentMessages(6)
            val recentHistory = recentEntities.reversed().map { "${it.sender}: ${it.text}" }

            val response = aiRepository.processQuery(
                query = query,
                apiKey = _apiKey.value,
                memoryContext = memoryContext,
                recentHistory = recentHistory,
                preferredLanguage = _selectedLanguage.value
            )

            _statusText.value = response.replyText

            // If action is proposed
            if (response.actionProposal != null) {
                val action = response.actionProposal
                if (action.requiresConfirmation) {
                    _pendingAction.value = action
                    _jarvisState.value = JarvisState.CONFIRMATION_REQUIRED
                    speak(response.replyText, response.detectedLanguage)
                } else {
                    executeAction(action, response.replyText, response.detectedLanguage)
                }
            } else {
                _jarvisState.value = JarvisState.SPEAKING
                speak(response.replyText, response.detectedLanguage)
                saveJarvisMessage(response.replyText, response.detectedLanguage, null, null)
            }
        }
    }

    fun confirmPendingAction(action: ActionProposal) {
        _pendingAction.value = null
        executeAction(action, "Executing authorized action...", "en")
    }

    fun cancelPendingAction() {
        _pendingAction.value = null
        _jarvisState.value = if (speechManager.isContinuousMode.value) JarvisState.LISTENING else JarvisState.IDLE
        val cancelMsg = "Action aborted."
        _statusText.value = cancelMsg
        speak(cancelMsg, "en")
    }

    private fun executeAction(action: ActionProposal, announcement: String, lang: String) {
        _jarvisState.value = JarvisState.EXECUTING
        viewModelScope.launch(Dispatchers.IO) {
            val result = actionExecutor.execute(action)
            _statusText.value = "${result.message} ${result.detail ?: ""}".trim()

            // If action created a reminder, also save in Room DB
            if (action.actionId == com.example.action.ActionRegistry.ACTION_CREATE_REMINDER && result.success) {
                val title = action.parameters["title"] ?: "Task"
                val timeStr = action.parameters["time_str"] ?: "Today"
                database.reminderDao().insertReminder(
                    ReminderEntity(
                        title = title,
                        timestamp = System.currentTimeMillis() + (3600 * 1000),
                        timeFormatted = timeStr
                    )
                )
            }

            saveJarvisMessage(result.message, lang, action.actionId, result.success)
            speak(result.message, lang)
        }
    }

    private fun speak(text: String, languageCode: String) {
        _jarvisState.value = JarvisState.SPEAKING
        ttsManager.speak(text, languageCode)
    }

    private suspend fun saveJarvisMessage(text: String, language: String, actionId: String?, actionSuccess: Boolean?) {
        database.conversationDao().insertMessage(
            ChatMessageEntity(
                sender = "JARVIS",
                text = text,
                language = language,
                actionId = actionId,
                actionSuccess = actionSuccess
            )
        )
    }

    // Reminders actions
    fun addReminder(title: String, type: String, time: String) {
        viewModelScope.launch {
            database.reminderDao().insertReminder(
                ReminderEntity(
                    title = title,
                    type = type,
                    timestamp = System.currentTimeMillis() + (3600 * 1000),
                    timeFormatted = time
                )
            )
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch {
            database.reminderDao().markCompleted(id)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            database.reminderDao().deleteReminderById(id)
        }
    }

    // Workflow actions
    fun runWorkflow(workflow: WorkflowEntity) {
        viewModelScope.launch {
            workflowEngine.executeWorkflow(workflow) { status ->
                _statusText.value = status
            }
        }
    }

    // Memory actions
    fun addMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            database.memoryDao().insertOrUpdateMemory(
                MemoryEntity(category = category, key = key, value = value)
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            database.memoryDao().deleteMemoryById(id)
        }
    }

    fun clearAllMemory() {
        viewModelScope.launch {
            database.memoryDao().clearAllMemory()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            database.conversationDao().clearAllMessages()
        }
    }

    fun updateVoiceSettings(rate: Float, pitch: Float) {
        ttsManager.setSpeedAndPitch(rate, pitch)
    }

    fun testVoice() {
        val testText = when (_selectedLanguage.value) {
            AssistantLanguage.TELUGU -> "నమస్కారం! నా వాయిస్ సిస్టమ్ విజయవంతంగా కాన్ఫిగర్ చేయబడింది."
            AssistantLanguage.HINDI -> "नमस्ते! मेरी आवाज़ प्रणाली ठीक से काम कर रही है।"
            AssistantLanguage.TANGLISH -> "Namaskaram! JARVIS voice synthesis working perfectly."
            else -> "Voice synthesis calibration test. All audio frequencies nominal."
        }
        speak(testText, _selectedLanguage.value.code)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
