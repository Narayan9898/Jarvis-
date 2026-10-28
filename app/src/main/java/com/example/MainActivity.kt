package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.JarvisTopBar
import com.example.ui.screens.AppsLauncherScreen
import com.example.ui.screens.ChatHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkflowsScreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTheme
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                JarvisApp(viewModel)
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val jarvisState by viewModel.jarvisState.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    val rmsVolume by viewModel.rmsVolume.collectAsStateWithLifecycle()
    val partialSpeech by viewModel.partialSpeech.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val isExecutingWorkflow by viewModel.isExecutingWorkflow.collectAsStateWithLifecycle()
    val workflowStatus by viewModel.workflowStatus.collectAsStateWithLifecycle()

    val messages by viewModel.messagesFlow.collectAsStateWithLifecycle()
    val reminders by viewModel.remindersFlow.collectAsStateWithLifecycle()
    val workflows by viewModel.workflowsFlow.collectAsStateWithLifecycle()
    val memoryList by viewModel.memoryFlow.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    // Handle back button when on subscreen
    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(Screen.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            JarvisTopBar(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { viewModel.setLanguage(it) },
                onSettingsClick = { viewModel.navigateTo(Screen.SETTINGS) },
                onPermissionsClick = { viewModel.navigateTo(Screen.PERMISSIONS) },
                hasApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurface,
                modifier = Modifier
                    .border(width = 0.5.dp, color = Color(0xFF16253D))
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Core HUD") },
                    label = { Text("Core", fontSize = 11.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.APPS,
                    onClick = {
                        viewModel.loadInstalledApps()
                        viewModel.navigateTo(Screen.APPS)
                    },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("Apps", fontSize = 11.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_apps")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.REMINDERS,
                    onClick = { viewModel.navigateTo(Screen.REMINDERS) },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = "Reminders") },
                    label = { Text("Alerts", fontSize = 11.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_reminders")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.WORKFLOWS,
                    onClick = { viewModel.navigateTo(Screen.WORKFLOWS) },
                    icon = { Icon(Icons.Default.AutoMode, contentDescription = "Routines") },
                    label = { Text("Routines", fontSize = 11.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_workflows")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.HISTORY,
                    onClick = { viewModel.navigateTo(Screen.HISTORY) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Logs") },
                    label = { Text("Logs", fontSize = 11.sp) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.DASHBOARD -> DashboardScreen(
                jarvisState = jarvisState,
                statusText = statusText,
                partialSpeech = partialSpeech,
                rmsVolume = rmsVolume,
                isSpeaking = isSpeaking,
                pendingAction = pendingAction,
                onStartVoice = { viewModel.startListening() },
                onStopVoice = { viewModel.stopListening() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onSendText = { viewModel.sendTextQuery(it) },
                onConfirmAction = { viewModel.confirmPendingAction(it) },
                onCancelAction = { viewModel.cancelPendingAction() },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.APPS -> AppsLauncherScreen(
                appsList = installedApps,
                onLaunchApp = { pkg ->
                    val proposal = com.example.data.model.ActionProposal(
                        actionId = com.example.action.ActionRegistry.ACTION_OPEN_APP,
                        name = "Open $pkg",
                        description = "Launch app",
                        parameters = mapOf("package_name" to pkg, "app_name" to pkg)
                    )
                    viewModel.confirmPendingAction(proposal)
                },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.REMINDERS -> RemindersScreen(
                reminders = reminders,
                onAddReminder = { title, type, time ->
                    viewModel.addReminder(title, type, time)
                },
                onCompleteReminder = { viewModel.completeReminder(it) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.WORKFLOWS -> WorkflowsScreen(
                workflows = workflows,
                isExecuting = isExecutingWorkflow,
                currentStepStatus = workflowStatus,
                onExecuteWorkflow = { viewModel.runWorkflow(it) },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.HISTORY -> ChatHistoryScreen(
                messages = messages,
                onClearHistory = { viewModel.clearChatHistory() },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.PERMISSIONS -> PermissionsScreen(
                modifier = Modifier.padding(innerPadding)
            )

            Screen.MEMORY -> MemoryScreen(
                memories = memoryList,
                onAddMemory = { cat, k, v -> viewModel.addMemory(cat, k, v) },
                onDeleteMemory = { viewModel.deleteMemory(it) },
                onClearAllMemory = { viewModel.clearAllMemory() },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.SETTINGS -> SettingsScreen(
                currentApiKey = apiKey,
                onSaveApiKey = { viewModel.setApiKey(it) },
                onUpdateVoiceSettings = { rate, pitch -> viewModel.updateVoiceSettings(rate, pitch) },
                onTestVoice = { viewModel.testVoice() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = JarvisCyan,
    selectedTextColor = JarvisCyan,
    unselectedIconColor = Color(0xFF6B83A8),
    unselectedTextColor = Color(0xFF6B83A8),
    indicatorColor = Color(0xFF0F2642)
)
