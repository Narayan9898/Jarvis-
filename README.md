# JARVIS AI — Advanced Personal Android Assistant

A production-grade, secure Android AI personal assistant inspired by fictional JARVIS. JARVIS enables voice commands, natural language processing, device automation, multilingual conversation (English, Telugu, Hindi, and Tanglish), and safe system control.

---

## 1. System Architecture

```
                                 [ USER ]
                                     |
                         Voice (Mic) / Text Input
                                     |
                                     v
                        +--------------------------+
                        |      SpeechManager       |
                        |   (SpeechRecognizer /    |
                        |    Audio RMS Metering)   |
                        +--------------------------+
                                     |
                                     v
                        +--------------------------+
                        |       AiRepository       |
                        |  - Multilingual NLU      |
                        |  - Intent Classification |
                        |  - Gemini API Client     |
                        +--------------------------+
                                     |
             +-----------------------+-----------------------+
             |                                               |
             v (Device Action)                               v (Conversation)
+--------------------------+                       +--------------------------+
|      ActionRegistry      |                       |        TtsManager        |
|  - JSON Schema Validator |                       |  (TextToSpeech Engine:   |
|  - Risk Classification   |                       |   en-US, te-IN, hi-IN)   |
+--------------------------+                       +--------------------------+
             |                                               |
             v                                               v
+--------------------------+                       +--------------------------+
|      ActionExecutor      |                       |       ArcReactorCore     |
|  - Android Intents       |                       |   (Animated HUD Core &   |
|  - PackageManager        |                       |    Audio Waveform)       |
|  - AlarmClock / Contacts |                       +--------------------------+
+--------------------------+
```

---

## 2. Supported Languages
- **English** (`en-US`, `en-IN`)
- **Telugu (తెలుగు)** (`te-IN`): Native script comprehension & TTS output.
- **Hindi (हिन्दी)** (`hi-IN`): Native Devanagari script comprehension & TTS output.
- **Tanglish** (`tanglish`): Telugu-English code-mixed phrases (e.g., *"YouTube open cheyi"*, *"Alarm pettuko"*).

---

## 3. Action Registry & Security Allowlist

The application strictly forbids running arbitrary scripts, shell commands, or unverified code. All actions are routed through `ActionRegistry` and validated against structured schemas:

| Action ID | Display Name | Risk Level | Requires Confirmation | Parameters |
|---|---|---|---|---|
| `open_app` | Open Application | LOW | No | `app_name`, `package_name` |
| `search_web` | Search Google Web | LOW | No | `query` |
| `set_alarm` | Set Alarm | MEDIUM | Yes | `hour`, `minutes`, `message` |
| `create_reminder`| Log Reminder | MEDIUM | No | `title`, `time_str` |
| `make_call` | Prepare Phone Call | MEDIUM | Yes (Dialer) | `contact_or_number` |
| `send_sms` | Draft SMS | MEDIUM | Yes (User Review) | `recipient`, `message` |
| `open_settings` | System Settings | LOW | No | `setting` (wifi, bluetooth, battery) |
| `toggle_torch` | Flashlight | LOW | No | `state` (true/false) |
| `open_camera` | Viewfinder | LOW | No | None |
| `run_workflow` | Routine Engine | LOW | No | `workflow_name` |

---

## 4. Permission Matrix

JARVIS implements least-privilege, on-demand permission handling with full user explanations:

- `RECORD_AUDIO`: Required for push-to-talk speech recognition.
- `READ_CONTACTS`: Required to resolve contact names when user requests a call or SMS.
- `CALL_PHONE`: Prepares system dialer calls.
- `READ_CALENDAR` / `WRITE_CALENDAR`: For calendar schedule review and morning routines.
- `CAMERA`: For hardware flashlight and camera viewfinder.
- `POST_NOTIFICATIONS`: For reminder and alarm notifications.

---

## 5. Production Backend Deployment Guide

For secure production deployment where Gemini API keys are never exposed in client binaries:

```
Android Client App  --->  Authenticated Backend  --->  Google Gemini API
(JWT / App Check)        (Node.js / Ktor / Cloud)      (Server Secret)
```

1. **Backend Environment Variables**:
   ```bash
   PORT=8080
   GEMINI_API_KEY="AIzaSy..."
   RATE_LIMIT_PER_MINUTE=60
   ```
2. **Backend Responsibilities**:
   - Verify Firebase App Check or JWT token from client.
   - Enforce per-user rate limits and request size ceilings.
   - Forward structured prompts to Gemini API (`gemini-2.0-flash`).
   - Validate proposed action schemas before responding to the mobile client.

---

## 6. Build Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK 36 (Android 16 DP / Android 15 compatible)

### Debug Build
```bash
gradle assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Release AAB (Android App Bundle)
```bash
gradle bundleRelease
```
