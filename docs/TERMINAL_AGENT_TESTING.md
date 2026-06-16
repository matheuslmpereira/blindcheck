# Terminal Agent Testing

This guide is for coding agents that need to test an Android app with BlindCheck using only terminal commands.

Use this when the desktop remote is not needed. The feedback comes from `adb logcat` through the `BlindCheckAnnounce` tag.

## Requirements

* One Android emulator or device visible in `adb devices`.
* TalkBack installed on the device. The default emulator image used by this project includes Google TalkBack.
* BlindCheck apps built from this repository.

Check the connected device:

```bash
make devices
```

## Start A Terminal-Only Session

For a fresh session:

```bash
make terminal-session
```

This runs:

1. `install-all`
2. `enable-tts`
3. `enable-tracker`
4. `open-test-app`

It does not start `blindcheck-desktop`.

For an already configured emulator:

```bash
make resume-terminal-session
```

Use the resume target when APKs are already installed. It avoids reinstalling the tracking app and reduces avoidable TalkBack/TTS process churn.

## Read Feedback In The Terminal

Open a dedicated terminal for logs:

```bash
make logs
```

Equivalent raw command:

```bash
adb logcat -s BlindCheckRemote:D BlindCheckTracker:D BlindCheckAnnounce:I
```

The important tag is `BlindCheckAnnounce`.

## Log Prefixes

BlindCheck emits these log messages:

| Prefix | Meaning | Example |
|---|---|---|
| `FOCUS` | Accessibility focus moved to an element | `FOCUS E-mail, Campo de texto, editavel` |
| `ANN` | Announcement or dynamic feedback inferred from accessibility events | `ANN Informe o e-mail` |
| `WIN` | Window or screen change | `WIN Acessar conta` |
| `TTS` | Text received by the BlindCheck `TextToSpeechService` | `TTS Acessar conta` |
| `EARCON` | Inferred non-speech feedback, such as navigation boundary | `EARCON boundary-next` |

`TTS` appears only when the BlindCheck TTS engine is selected as the system TTS engine. `make terminal-session`, `make resume-terminal-session`, and `make enable-tts` select it.

`EARCON` is not raw audio capture. It is inferred when a remote `next` or `previous` action does not produce a new focus event within the configured timeout.

## Validate TTS Capture

After enabling the session, clear old logs and run a smoke request:

```bash
adb logcat -c
make tts-smoke
sleep 2
adb logcat -d -s BlindCheckAnnounce:I
```

Expected evidence:

```text
TTS_SMOKE_REQUEST default BlindCheck_TTS_smoke_test
TTS BlindCheck_TTS_smoke_test
```

If `TTS` does not appear, check:

```bash
make check-a11y
```

Expected:

* `enabled_accessibility_services` includes TalkBack and BlindCheck.
* `accessibility_enabled` is `1`.
* `tts_default_synth` is `com.theustech.blindcheck_tracking_app`.

## Navigate By Terminal

Use these commands to drive TalkBack-style navigation:

```bash
make next
make previous
make activate
make back
make scroll-forward
make scroll-backward
make swipe-up
make swipe-down
make home
make recents
```

For each action, inspect the log terminal. A typical `next` action can produce:

```text
BlindCheckRemote: Received remote action: com.theustech.blindcheck.ACTION_NEXT
BlindCheckTracker: Executing remote action: com.theustech.blindcheck.ACTION_NEXT
BlindCheckAnnounce: FOCUS E-mail, Campo de texto, editavel
BlindCheckAnnounce: TTS E-mail
BlindCheckAnnounce: TTS Edit box.
```

## Recommended Agent Loop

Use this loop when testing another Android app:

1. Install or launch the app under test.
2. Start BlindCheck terminal mode:

```bash
make terminal-session
```

3. Clear logs before the scenario:

```bash
adb logcat -c
```

4. Execute one user-level action:

```bash
make next
```

5. Collect evidence:

```bash
adb logcat -d -s BlindCheckAnnounce:I BlindCheckRemote:D BlindCheckTracker:D
```

6. Assert against observed behavior:

* focus order via `FOCUS`;
* actual captured TTS text via `TTS`;
* screen transitions via `WIN`;
* boundary feedback via `EARCON`.

7. Repeat one action at a time. Avoid sending large command bursts unless the test is intentionally checking rapid navigation.

## Testing A Different App

`make open-test-app` opens the bundled sample app. For another app, launch it directly:

```bash
adb shell monkey -p your.app.package -c android.intent.category.LAUNCHER 1
```

Then keep using the same BlindCheck navigation commands.

If the app has login or sensitive fields, do not paste raw passwords into logs or final reports. Accessibility and TTS logs can contain visible labels, hints, and user-entered text.

## Report Format For Agents

When finishing a terminal validation, report:

```text
Validated:
- setup command:
- navigation commands:
- log evidence:
- result:
- limitations:
```

Include the relevant `FOCUS`, `TTS`, `WIN`, and `EARCON` lines. Do not claim exact TalkBack audio fidelity unless the evidence is a `TTS` line from the BlindCheck engine.

## Troubleshooting

If TTS lines are missing:

```bash
make enable-tts
make enable-tracker
make tts-smoke
```

If the same operational TTS message repeats many times, avoid repeatedly reinstalling while TalkBack is active. Use:

```bash
make resume-terminal-session
```

If navigation commands are received but focus does not move, verify TalkBack and BlindCheck are both enabled:

```bash
make check-a11y
```

If logs are noisy, clear them before each scenario:

```bash
adb logcat -c
```
