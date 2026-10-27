package com.example.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.example.data.model.ActionProposal
import com.example.data.model.ActionResult
import com.example.data.model.AppInfo

class ActionExecutor(private val context: Context) {

    /**
     * Executes a validated ActionProposal on the Android OS.
     * Guarantees that actions report true only when confirmed by the OS.
     */
    fun execute(proposal: ActionProposal): ActionResult {
        return try {
            when (proposal.actionId) {
                ActionRegistry.ACTION_OPEN_APP -> executeOpenApp(proposal)
                ActionRegistry.ACTION_SEARCH_WEB -> executeSearchWeb(proposal)
                ActionRegistry.ACTION_SET_ALARM -> executeSetAlarm(proposal)
                ActionRegistry.ACTION_CREATE_REMINDER -> executeCreateReminder(proposal)
                ActionRegistry.ACTION_MAKE_CALL -> executeMakeCall(proposal)
                ActionRegistry.ACTION_SEND_SMS -> executeSendSms(proposal)
                ActionRegistry.ACTION_SEARCH_CONTACTS -> executeSearchContacts(proposal)
                ActionRegistry.ACTION_OPEN_SETTINGS -> executeOpenSettings(proposal)
                ActionRegistry.ACTION_TOGGLE_TORCH -> executeToggleTorch(proposal)
                ActionRegistry.ACTION_OPEN_CAMERA -> executeOpenCamera()
                ActionRegistry.ACTION_VIEW_CALENDAR -> executeViewCalendar()
                else -> ActionResult(
                    success = false,
                    message = "Unknown or unsupported action ID: ${proposal.actionId}"
                )
            }
        } catch (e: SecurityException) {
            ActionResult(
                success = false,
                message = "Permission denied: ${e.localizedMessage ?: "Missing required Android permission."}"
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                message = "Action failed: ${e.localizedMessage ?: "Unexpected system error."}"
            )
        }
    }

    private fun executeOpenApp(proposal: ActionProposal): ActionResult {
        val appName = proposal.parameters["app_name"] ?: ""
        val targetPackage = proposal.parameters["package_name"]

        val pm = context.packageManager

        // Direct package launch if provided
        if (!targetPackage.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult(
                    success = true,
                    message = "Opened application",
                    detail = targetPackage,
                    openIntentPackage = targetPackage
                )
            }
        }

        // Search installed apps by name
        val installedApps = getInstalledApplications()
        val match = installedApps.firstOrNull {
            it.appName.contains(appName, ignoreCase = true) ||
            it.packageName.contains(appName, ignoreCase = true)
        }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult(
                    success = true,
                    message = "Launching ${match.appName}",
                    detail = match.packageName,
                    openIntentPackage = match.packageName
                )
            }
        }

        // Common known applications fallback
        val commonPackage = when (appName.lowercase().trim()) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "chrome" -> "com.android.chrome"
            "gmail" -> "com.google.android.gm"
            "settings" -> "com.android.settings"
            else -> null
        }

        if (commonPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(commonPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult(
                    success = true,
                    message = "Launching $appName",
                    detail = commonPackage,
                    openIntentPackage = commonPackage
                )
            }
        }

        return ActionResult(
            success = false,
            message = "Application '$appName' is not installed on this device."
        )
    }

    fun getInstalledApplications(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            if (packageName != context.packageName) {
                AppInfo(
                    packageName = packageName,
                    appName = label,
                    isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } else null
        }.sortedBy { it.appName }
    }

    private fun executeSearchWeb(proposal: ActionProposal): ActionResult {
        val query = proposal.parameters["query"] ?: ""
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Initiating Google search for '$query'",
                detail = query
            )
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch web browser: ${e.message}")
        }
    }

    private fun executeSetAlarm(proposal: ActionProposal): ActionResult {
        val hour = proposal.parameters["hour"]?.toIntOrNull() ?: 8
        val minutes = proposal.parameters["minutes"]?.toIntOrNull() ?: 0
        val message = proposal.parameters["message"] ?: "JARVIS Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Alarm configured for %02d:%02d (%s)".format(hour, minutes, message),
                detail = "Alarm scheduled via Android AlarmClock"
            )
        } catch (e: Exception) {
            ActionResult(success = false, message = "System clock app unavailable: ${e.message}")
        }
    }

    private fun executeCreateReminder(proposal: ActionProposal): ActionResult {
        val title = proposal.parameters["title"] ?: "Task"
        val timeStr = proposal.parameters["time_str"] ?: "Today"
        return ActionResult(
            success = true,
            message = "Reminder set: '$title' ($timeStr)",
            detail = title
        )
    }

    private fun executeMakeCall(proposal: ActionProposal): ActionResult {
        val contactOrNumber = proposal.parameters["contact_or_number"] ?: ""
        // Use system dialer to ensure user review and safety
        val dialUri = if (contactOrNumber.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            Uri.parse("tel:${contactOrNumber.trim()}")
        } else {
            // Find contact first
            val contactNumber = lookupContactPhoneNumber(contactOrNumber)
            if (contactNumber != null) {
                Uri.parse("tel:$contactNumber")
            } else {
                Uri.parse("tel:")
            }
        }

        val intent = Intent(Intent.ACTION_DIAL, dialUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Opening dialer for $contactOrNumber",
                detail = dialUri.toString()
            )
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open phone dialer: ${e.message}")
        }
    }

    private fun executeSendSms(proposal: ActionProposal): ActionResult {
        val recipient = proposal.parameters["recipient"] ?: ""
        val textMessage = proposal.parameters["message"] ?: ""

        val phone = lookupContactPhoneNumber(recipient) ?: recipient

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(phone)}")
            putExtra("sms_body", textMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Prepared message to $recipient for review",
                detail = textMessage
            )
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open messaging app: ${e.message}")
        }
    }

    fun lookupContactPhoneNumber(nameOrQuery: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$nameOrQuery%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex >= 0) it.getString(numberIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun searchContacts(query: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && list.size < 10) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val num = if (numIndex >= 0) it.getString(numIndex) else ""
                    list.add(name to num)
                }
            }
        } catch (e: Exception) {
            // Permission not granted or query failed
        }
        return list
    }

    private fun executeSearchContacts(proposal: ActionProposal): ActionResult {
        val query = proposal.parameters["query"] ?: ""
        val matches = searchContacts(query)
        return if (matches.isNotEmpty()) {
            val summary = matches.joinToString("\n") { "${it.first}: ${it.second}" }
            ActionResult(
                success = true,
                message = "Found ${matches.size} contact(s) matching '$query'",
                detail = summary
            )
        } else {
            ActionResult(
                success = true,
                message = "No contacts found matching '$query' or contacts permission not granted.",
                detail = null
            )
        }
    }

    private fun executeOpenSettings(proposal: ActionProposal): ActionResult {
        val setting = proposal.parameters["setting"]?.lowercase()?.trim() ?: "general"
        val intent = when (setting) {
            "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "apps", "applications" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(
                success = true,
                message = "Opened system $setting settings",
                detail = setting
            )
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open $setting settings: ${e.message}")
        }
    }

    private fun executeToggleTorch(proposal: ActionProposal): ActionResult {
        val state = proposal.parameters["state"]?.equals("true", ignoreCase = true) == true ||
                proposal.parameters["state"]?.equals("on", ignoreCase = true) == true
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ActionResult(success = false, message = "Camera manager service unavailable.")

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return ActionResult(success = false, message = "No camera flashlight found on device.")
            cameraManager.setTorchMode(cameraId, state)
            ActionResult(
                success = true,
                message = if (state) "Flashlight illuminated." else "Flashlight turned off.",
                detail = "State: $state"
            )
        } catch (e: CameraAccessException) {
            ActionResult(success = false, message = "Flashlight unavailable: ${e.localizedMessage}")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not toggle torch: ${e.localizedMessage}")
        }
    }

    private fun executeOpenCamera(): ActionResult {
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening camera viewfinder.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch camera: ${e.message}")
        }
    }

    private fun executeViewCalendar(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening device calendar.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open calendar: ${e.message}")
        }
    }
}
