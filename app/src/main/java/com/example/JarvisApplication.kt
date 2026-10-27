package com.example

import android.app.Application
import com.example.action.ActionExecutor
import com.example.data.local.JarvisDatabase
import com.example.data.repository.AiRepository
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import com.example.workflow.WorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class JarvisApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { JarvisDatabase.getDatabase(this, applicationScope) }
    val actionExecutor by lazy { ActionExecutor(this) }
    val speechManager by lazy { SpeechManager(this) }
    val ttsManager by lazy { TtsManager(this) }
    val aiRepository by lazy { AiRepository() }
    val workflowEngine by lazy {
        WorkflowEngine(database, actionExecutor, ttsManager)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
