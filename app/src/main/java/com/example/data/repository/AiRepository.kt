package com.example.data.repository

import android.util.Log
import com.example.action.ActionRegistry
import com.example.data.model.ActionProposal
import com.example.data.model.AssistantLanguage
import com.example.data.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AiResponse(
    val replyText: String,
    val detectedLanguage: String,
    val actionProposal: ActionProposal? = null
)

class AiRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val languageDetector = LanguageDetector()

    /**
     * Determines intent, language, and structured action.
     * Uses Gemini API if API key is provided, or uses the local offline NLU engine.
     */
    suspend fun processQuery(
        query: String,
        apiKey: String,
        memoryContext: String = "",
        recentHistory: List<String> = emptyList(),
        preferredLanguage: AssistantLanguage = AssistantLanguage.AUTO
    ): AiResponse = withContext(Dispatchers.IO) {
        // Step 1: Detect user's input language and build context-aware Gemini directive
        val langResult = languageDetector.analyze(query, preferredLanguage)
        val detectedLang = langResult.languageCode

        // Step 2: Try local high-confidence direct device action matcher first for speed and privacy
        val localAction = extractLocalAction(query)
        if (localAction != null) {
            val reply = generateActionConfirmationMessage(localAction, detectedLang)
            return@withContext AiResponse(
                replyText = reply,
                detectedLanguage = detectedLang,
                actionProposal = localAction
            )
        }

        // Step 3: If an API key is available, execute context-aware Gemini generation
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiResult = callGeminiApi(
                    query = query,
                    apiKey = apiKey,
                    memoryContext = memoryContext,
                    langDirective = langResult.geminiDirective,
                    detectedLang = detectedLang,
                    recentHistory = recentHistory
                )
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.w("AiRepository", "Gemini API failed, falling back to local reasoning: ${e.message}")
            }
        }

        // Step 4: Offline conversational intelligence & local knowledge
        val offlineReply = generateLocalKnowledgeReply(query, detectedLang)
        AiResponse(
            replyText = offlineReply,
            detectedLanguage = detectedLang,
            actionProposal = null
        )
    }

    fun detectLanguage(text: String): String {
        return languageDetector.analyze(text).languageCode
    }

    /**
     * Local deterministic intent parser for instant device actions
     * Supports English, Telugu, Hindi, and Tanglish.
     */
    fun extractLocalAction(query: String): ActionProposal? {
        val lower = query.lowercase().trim()

        // 1. OPEN APP
        // "open youtube", "open whatsapp", "ఓపెన్ చేయి", "खोलो"
        val openAppKeywords = listOf("open", "launch", "start", "ఓపెన్ చేయి", "ఓపెన్ చెయ్", "खोलो", "चालू करो")
        for (kw in openAppKeywords) {
            if (lower.startsWith(kw) || lower.contains(kw)) {
                var appName = lower.replace(kw, "").replace("app", "").replace("యాప్", "").trim()
                if (appName.startsWith("the ")) appName = appName.removePrefix("the ")
                if (appName.isNotBlank() && appName.length < 30) {
                    return ActionProposal(
                        actionId = ActionRegistry.ACTION_OPEN_APP,
                        name = "Open $appName",
                        description = "Launch installed application: $appName",
                        parameters = mapOf("app_name" to appName),
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false,
                        rawQuery = query
                    )
                }
            }
        }

        // 2. SET ALARM
        // "set an alarm for 6 am", "alarm at 7:30", "ఉదయం 6 గంటలకు అలారం పెట్టు", "अलार्म लगाओ"
        if (lower.contains("alarm") || lower.contains("అలారం") || lower.contains("अलार्म")) {
            val hourMinute = extractTime(lower)
            return ActionProposal(
                actionId = ActionRegistry.ACTION_SET_ALARM,
                name = "Set Alarm",
                description = "Set alarm for ${hourMinute.first}:${hourMinute.second}",
                parameters = mapOf(
                    "hour" to hourMinute.first.toString(),
                    "minutes" to hourMinute.second.toString(),
                    "message" to "JARVIS Alarm"
                ),
                riskLevel = RiskLevel.MEDIUM,
                requiresConfirmation = true,
                rawQuery = query
            )
        }

        // 3. CREATE REMINDER
        // "remind me to pay bills", "గుర్తు చేయి", "याद दिलाना"
        if (lower.contains("remind") || lower.contains("గుర్తు చేయి") || lower.contains("याद दिलाना") || lower.contains("reminder")) {
            val title = lower
                .replace("remind me to", "")
                .replace("remind me", "")
                .replace("గుర్తు చేయి", "")
                .replace("గుర్తుచెయ్యి", "")
                .replace("reminder for", "")
                .trim()
                .ifBlank { "Important Task" }

            return ActionProposal(
                actionId = ActionRegistry.ACTION_CREATE_REMINDER,
                name = "Create Reminder",
                description = "Task: $title",
                parameters = mapOf("title" to title, "time_str" to "Upcoming"),
                riskLevel = RiskLevel.MEDIUM,
                requiresConfirmation = false,
                rawQuery = query
            )
        }

        // 4. PHONE CALL
        // "call father", "call ravi", "ఫోన్ చేయి", "कॉल करो"
        if (lower.startsWith("call") || lower.contains("ఫోన్ చేయి") || lower.contains("कॉल करो") || lower.contains("dial")) {
            val target = lower
                .replace("call", "")
                .replace("dial", "")
                .replace("ఫోన్ చేయి", "")
                .replace("कॉल करो", "")
                .trim()
            if (target.isNotBlank()) {
                return ActionProposal(
                    actionId = ActionRegistry.ACTION_MAKE_CALL,
                    name = "Call $target",
                    description = "Dial contact or number: $target",
                    parameters = mapOf("contact_or_number" to target),
                    riskLevel = RiskLevel.MEDIUM,
                    requiresConfirmation = true,
                    rawQuery = query
                )
            }
        }

        // 5. SMS
        // "send message to ravi", "text ravi i am coming", "మెసేజ్ చేయి", "संदेश भेजो"
        if (lower.contains("message") || lower.contains("text ") || lower.contains("sms") || lower.contains("మెసేజ్")) {
            return ActionProposal(
                actionId = ActionRegistry.ACTION_SEND_SMS,
                name = "Send Message",
                description = "Draft text message",
                parameters = mapOf("recipient" to "Selected Contact", "message" to query),
                riskLevel = RiskLevel.MEDIUM,
                requiresConfirmation = true,
                rawQuery = query
            )
        }

        // 6. TORCH / FLASHLIGHT
        // "turn on torch", "flashlight on", "టార్చ్ వెలిగించు", "टॉर्च जलाओ"
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("టార్చ్") || lower.contains("टॉर्च")) {
            val enable = !lower.contains("off") && !lower.contains("ఆపు") && !lower.contains("बंद")
            return ActionProposal(
                actionId = ActionRegistry.ACTION_TOGGLE_TORCH,
                name = if (enable) "Turn On Torch" else "Turn Off Torch",
                description = "Toggle hardware flashlight",
                parameters = mapOf("state" to enable.toString()),
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false,
                rawQuery = query
            )
        }

        // 7. SETTINGS
        // "open wifi settings", "open bluetooth", "సెట్టింగ్స్"
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return ActionProposal(
                actionId = ActionRegistry.ACTION_OPEN_SETTINGS,
                name = "Open Wi-Fi Settings",
                description = "Configure wireless networking",
                parameters = mapOf("setting" to "wifi"),
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false,
                rawQuery = query
            )
        }
        if (lower.contains("bluetooth")) {
            return ActionProposal(
                actionId = ActionRegistry.ACTION_OPEN_SETTINGS,
                name = "Open Bluetooth Settings",
                description = "Configure bluetooth devices",
                parameters = mapOf("setting" to "bluetooth"),
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false,
                rawQuery = query
            )
        }

        // 8. CAMERA
        if (lower.contains("camera") || lower.contains("ఫోటో తీయి") || lower.contains("కెమెరా") || lower.contains("कैमरा")) {
            return ActionProposal(
                actionId = ActionRegistry.ACTION_OPEN_CAMERA,
                name = "Open Camera",
                description = "Launch camera viewfinder",
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false,
                rawQuery = query
            )
        }

        // 9. WEB SEARCH
        // "search for weather", "who is elon musk", "గూగుల్ లో వెతుకు", "खोजो"
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("వెతుకు") || lower.contains("search for")) {
            val q = lower.replace("search for", "").replace("search", "").replace("google", "").trim()
            return ActionProposal(
                actionId = ActionRegistry.ACTION_SEARCH_WEB,
                name = "Web Search",
                description = "Google search query: $q",
                parameters = mapOf("query" to q),
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false,
                rawQuery = query
            )
        }

        return null
    }

    private fun extractTime(text: String): Pair<Int, Int> {
        val pattern = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 8
            val minute = matcher.group(3)?.toIntOrNull() ?: 0
            val ampm = matcher.group(4)?.lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, minute)
        }
        return Pair(7, 0)
    }

    private fun generateActionConfirmationMessage(action: ActionProposal, lang: String): String {
        return when (lang) {
            "te" -> when (action.actionId) {
                ActionRegistry.ACTION_OPEN_APP -> "${action.parameters["app_name"]} యాప్ ఓపెన్ చేస్తున్నాను..."
                ActionRegistry.ACTION_SET_ALARM -> "అలారం సెట్ చేయడానికి సిద్ధంగా ఉంది."
                ActionRegistry.ACTION_CREATE_REMINDER -> "రిమైండర్ సేవ్ చేయబడింది: ${action.parameters["title"]}"
                ActionRegistry.ACTION_TOGGLE_TORCH -> "టార్చ్ స్థితి మార్చబడింది."
                else -> "సరే, మీ ఆదేశాన్ని నిర్వర్తిస్తున్నాను."
            }
            "hi" -> when (action.actionId) {
                ActionRegistry.ACTION_OPEN_APP -> "${action.parameters["app_name"]} ऐप खोला जा रहा है..."
                ActionRegistry.ACTION_SET_ALARM -> "अलार्म सेट करने के लिए तैयार।"
                ActionRegistry.ACTION_CREATE_REMINDER -> "रिमाइंडर सहेज लिया गया है।"
                else -> "जी, आपका आदेश पूरा किया जा रहा है।"
            }
            "tanglish" -> when (action.actionId) {
                ActionRegistry.ACTION_OPEN_APP -> "${action.parameters["app_name"]} app ni open chestunnanu."
                ActionRegistry.ACTION_SET_ALARM -> "Alarm set cheyyadaniki ready ga undhi."
                else -> "Sure! Mee command execute chestunnanu."
            }
            else -> when (action.actionId) {
                ActionRegistry.ACTION_OPEN_APP -> "Right away, sir. Launching ${action.parameters["app_name"]}."
                ActionRegistry.ACTION_SET_ALARM -> "Preparing your alarm as requested."
                ActionRegistry.ACTION_CREATE_REMINDER -> "Reminder logged: ${action.parameters["title"]}."
                ActionRegistry.ACTION_MAKE_CALL -> "Preparing phone call."
                ActionRegistry.ACTION_SEARCH_WEB -> "Searching the web for '${action.parameters["query"]}'."
                else -> "Executing requested protocol: ${action.name}."
            }
        }
    }

    private fun generateLocalKnowledgeReply(query: String, lang: String): String {
        val lower = query.lowercase().trim()

        if (lower.contains("who are you") || lower.contains("ఎవరు") || lower.contains("तुम कौन हो")) {
            return when (lang) {
                "te" -> "నేను JARVIS ని. మీ అధునాతన వ్యక్తిగత Android సహాయకుడిని. యాప్స్ ఓపెన్ చేయడం, అలారాలు, రిమైండర్లు, కాల్స్ మరియు AI సమాధానాలలో మీకు సహాయం చేస్తాను."
                "hi" -> "मैं JARVIS हूँ, आपका व्यक्तिगत AI Android सहायक। मैं आपके डिवाइस नियंत्रण और सवालों के जवाब में मदद कर सकता हूँ।"
                "tanglish" -> "Nenu JARVIS ni. Mee personal Android AI assistant ni. Apps open cheyyadam, alarms, reminders and general doubts lo help chestanu."
                else -> "I am JARVIS, an advanced personal Android assistant. I can control your device functions, manage reminders, initiate calls, launch apps, and converse in multiple languages."
            }
        }

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("నమస్కారం")) {
            return when (lang) {
                "te" -> "నమస్కారం! నేను JARVIS. మీకు ఏం సహాయం చేయాలి?"
                "hi" -> "नमस्ते! मैं JARVIS हूँ। आज मैं आपकी क्या मदद कर सकता हूँ?"
                "tanglish" -> "Namaskaram! Nenu JARVIS ni. Emi sahayam kavali meeku?"
                else -> "Hello! I am JARVIS. All diagnostic parameters are green. How may I assist you today?"
            }
        }

        // Generic helpful assistant response
        return when (lang) {
            "te" -> "మీ ఆదేశాన్ని అర్థం చేసుకున్నాను. మీరు 'యూట్యూబ్ ఓపెన్ చేయి', 'రేపు 6 గంటలకు అలారం పెట్టు', లేదా 'టార్చ్ వెలిగించు' అని అడగవచ్చు."
            "hi" -> "मैंने आपका अनुरोध प्राप्त किया। आप मुझे ऐप खोलने, अलार्म लगाने, या सामान्य प्रश्न पूछने के लिए कह सकते हैं।"
            "tanglish" -> "Mee query receive cheskunnanu. Apps open cheyyadaniki, reminders pettadaniki, or questions adagadaniki nannu command ivvochu."
            else -> "Protocol acknowledged, sir. You can instruct me to launch any application, schedule alarms or reminders, inspect system settings, or query information."
        }
    }

    private suspend fun callGeminiApi(
        query: String,
        apiKey: String,
        memoryContext: String,
        langDirective: String,
        detectedLang: String,
        recentHistory: List<String> = emptyList()
    ): AiResponse? = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val historyBlock = if (recentHistory.isNotEmpty()) {
            "Recent Conversation History:\n" + recentHistory.takeLast(6).joinToString("\n") + "\n\n"
        } else ""

        val systemPrompt = """
            You are JARVIS, an advanced, highly capable, professional, and slightly witty personal assistant on Android.
            
            $langDirective
            
            Memory and User Preferences:
            $memoryContext
            
            $historyBlock
            If the user asks for a physical mobile phone action, you can output an action plan JSON inside an ```action``` block:
            ```action
            {"action": "open_app", "parameters": {"app_name": "youtube"}}
            ```
            Supported actions:
            - open_app (parameters: app_name)
            - search_web (parameters: query)
            - set_alarm (parameters: hour, minutes, message)
            - create_reminder (parameters: title, time_str)
            - make_call (parameters: contact_or_number)
            - send_sms (parameters: recipient, message)
            - open_settings (parameters: setting)
            - toggle_torch (parameters: state)
            - open_camera
            - view_calendar
            
            Keep your spoken response concise, polite, and helpful (under 3 sentences).
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser Query: $query"))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("AiRepository", "Gemini API HTTP ${response.code}: ${response.message}")
            return@withContext null
        }

        val responseString = response.body?.string() ?: return@withContext null
        val responseJson = JSONObject(responseString)
        val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
        if (candidates.length() == 0) return@withContext null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text") ?: return@withContext null

        // Parse any action block from Gemini response
        var actionProposal: ActionProposal? = null
        val actionPattern = Pattern.compile("```action\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL)
        val matcher = actionPattern.matcher(text)
        var cleanReply = text
        if (matcher.find()) {
            val jsonActionStr = matcher.group(1)
            try {
                val actJson = JSONObject(jsonActionStr ?: "")
                val actionId = actJson.optString("action")
                val paramsJson = actJson.optJSONObject("parameters")
                val paramsMap = mutableMapOf<String, String>()
                paramsJson?.keys()?.forEach { k ->
                    paramsMap[k] = paramsJson.optString(k)
                }
                actionProposal = ActionRegistry.validateProposal(
                    ActionProposal(
                        actionId = actionId,
                        name = actionId.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        description = "AI generated action plan",
                        parameters = paramsMap,
                        rawQuery = query
                    )
                )
            } catch (e: Exception) {
                Log.w("AiRepository", "Could not parse action JSON from Gemini: ${e.message}")
            }
            cleanReply = text.replace(actionPattern.toRegex(), "").trim()
        }

        AiResponse(
            replyText = cleanReply,
            detectedLanguage = detectedLang,
            actionProposal = actionProposal
        )
    }
}
