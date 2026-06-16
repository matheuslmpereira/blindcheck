# Makefile Targets

## Goal

Provide simple commands for local accessibility testing and agentic validation.

## Suggested targets

Session setup targets should be idempotent:

* do not rewrite `tts_default_synth` when the BlindCheck TTS engine is already selected;
* do not restart accessibility services when TalkBack and BlindCheck are already enabled;
* do not start another desktop process when one is already running.

Use a separate `resume-session` target for returning to an already configured emulator without reinstalling APKs. Reinstalling the tracking app can recreate the process that hosts the TTS engine and may cause TalkBack to announce the selected engine again.

```makefile
ADB=adb
TRACKING_SERVICE=com.theustech.blindcheck.tracker/.TrackingAccessibilityService
TTS_ENGINE=com.theustech.blindcheck_tracking_app

.PHONY: devices
devices:
	$(ADB) devices

.PHONY: install-all
install-all:
	./gradlew :blindcheck-tracking-app:installDebug :blindcheck-test-app:installDebug

.PHONY: enable-tracker
enable-tracker:
	$(ADB) shell settings put secure enabled_accessibility_services $(TRACKING_SERVICE)
	$(ADB) shell settings put secure accessibility_enabled 1

.PHONY: enable-tts
enable-tts:
	$(ADB) shell settings put secure tts_default_synth $(TTS_ENGINE)
	$(ADB) shell settings put secure tts_enabled_plugins $(TTS_ENGINE)

.PHONY: disable-a11y
disable-a11y:
	$(ADB) shell settings put secure enabled_accessibility_services null
	$(ADB) shell settings put secure accessibility_enabled 0

.PHONY: check-a11y
check-a11y:
	$(ADB) shell settings get secure enabled_accessibility_services
	$(ADB) shell settings get secure accessibility_enabled
	$(ADB) shell settings get secure tts_default_synth

.PHONY: open-a11y-settings
open-a11y-settings:
	$(ADB) shell am start -a android.settings.ACCESSIBILITY_SETTINGS

.PHONY: test
test:
	./gradlew connectedDebugAndroidTest

.PHONY: tts-smoke
tts-smoke:
	$(ADB) shell am broadcast -p com.theustech.blindcheck_tracking_app \
		-a com.theustech.blindcheck.ACTION_TTS_SMOKE \
		--es com.theustech.blindcheck.EXTRA_TTS_TEXT BlindCheck_TTS_smoke_test

.PHONY: validate
validate:
	./gradlew test connectedDebugAndroidTest
```

## Note

Some Android versions restrict direct modification of secure accessibility settings.

When direct ADB setup fails, the tool should open accessibility settings and guide the user.

Agents must report which validation command was run before considering a task complete.
