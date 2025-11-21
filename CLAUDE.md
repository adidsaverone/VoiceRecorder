# Voice Recorder - Hebrew Voice Notes App

Android app for recording voice notes in Hebrew and sending to WhatsApp, Google Docs, or Google Keep.

## Features

- **Voice Recording**: Hebrew speech-to-text using Google Speech Recognition API
- **WhatsApp Integration**: Send transcribed text directly to configured WhatsApp number
- **Google Docs Integration**: Automatically append notes to Google Doc via Apps Script webhook
- **Google Keep Integration**: Open Google Keep with pre-filled note and label
- **Dark Mode**: Complete dark theme UI
- **Configurable Settings**: WhatsApp number, Docs webhook URL, Notes label

## Architecture

### Main Components

- **MainActivity.kt** - Main voice recording and sending logic
- **SettingsActivity.kt** - Configuration screen for WhatsApp, Docs, Keep
- **NotesActivity.kt** - Local notes viewer (unused with Google Keep)

### Key Features

1. **Speech Recognition**
   - Uses Android SpeechRecognizer API
   - Hebrew language support (`he-IL`)
   - Readable error messages (Error 7 = "No speech recognized")

2. **Integrations**
   - WhatsApp: URL intent to `wa.me` API
   - Google Docs: HTTP POST to Apps Script webhook
   - Google Keep: ACTION_SEND intent with label as EXTRA_SUBJECT

3. **UI/UX**
   - Dark theme (#1a1a1a background)
   - Red circular record button
   - Right-to-left text display for Hebrew
   - Text box clears after sending

## Google Apps Script Setup

Deploy this script and add the webhook URL to app settings:

```javascript
function doPost(e) {
  var text = e.parameter.text || "no text";
  var doc = DocumentApp.openById("YOUR_DOC_ID");
  var body = doc.getBody();
  var today = new Date().toLocaleDateString('he-IL');
  var content = body.getText();

  // Add date header if not already present
  if (content.indexOf(today) === -1) {
    body.appendParagraph(today)
        .setAlignment(DocumentApp.HorizontalAlignment.RIGHT)
        .setBold(true);
  }

  // Add note text
  body.appendParagraph(text)
      .setAlignment(DocumentApp.HorizontalAlignment.RIGHT)
      .setBold(false);

  return ContentService.createTextOutput("OK");
}
```

**Deployment:**
1. Deploy → New deployment
2. Execute as: Me
3. Who has access: Anyone
4. Copy webhook URL to app settings

## Settings Configuration

**WhatsApp Number**: International format without + (e.g., `972544414686`)
**Google Docs Webhook**: Apps Script deployment URL
**Notes Label**: Label/title for Google Keep notes (e.g., "Work", "Personal")

## Error Messages

- **Error 1**: Network timeout
- **Error 2**: Network error
- **Error 3**: Audio error
- **Error 4**: Server error
- **Error 5**: Client error
- **Error 6**: No speech detected
- **Error 7**: No speech recognized
- **Error 8**: Recognizer busy
- **Error 9**: Permission denied

## Building

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## ADB Wireless

```bash
adb connect 10.100.102.17:5555
```

## Use Case

Designed for Dani's India tours travel business - quick voice notes for customer interactions, bookings, and follow-ups with automatic logging to WhatsApp, Google Docs, and Google Keep.

## Limitations

- Google Keep doesn't support automatic label assignment via intent (labels must be set manually in Keep)
- Google Docs requires publicly accessible Apps Script webhook
- WhatsApp integration opens WhatsApp app (not fully automatic)

## Technical Stack

- Kotlin
- Android SDK 33
- Google Speech Recognition API (on-device)
- Material Design components
- SharedPreferences for settings
