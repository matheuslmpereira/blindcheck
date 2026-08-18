ADB=adb
TRACKING_PKG=com.theustech.blindcheck_tracking_app
TRACKING_SERVICE=$(TRACKING_PKG)/com.theustech.blindcheck_interactor.TrackingAccessibilityService
TTS_ENGINE=$(TRACKING_PKG)
TALKBACK_SERVICE=com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
TALKBACK_PKG=com.google.android.marvin.talkback
TEST_APP_PKG=com.theustech.blindcheck_testeapp

ACTION_NEXT     = com.theustech.blindcheck.ACTION_NEXT
ACTION_PREVIOUS = com.theustech.blindcheck.ACTION_PREVIOUS
ACTION_ACTIVATE = com.theustech.blindcheck.ACTION_ACTIVATE
ACTION_BACK     = com.theustech.blindcheck.ACTION_BACK
ACTION_SCROLL_FORWARD  = com.theustech.blindcheck.ACTION_SCROLL_FORWARD
ACTION_SCROLL_BACKWARD = com.theustech.blindcheck.ACTION_SCROLL_BACKWARD
ACTION_HOME     = com.theustech.blindcheck.ACTION_HOME
ACTION_RECENTS  = com.theustech.blindcheck.ACTION_RECENTS
ACTION_SWIPE_UP   = com.theustech.blindcheck.ACTION_SWIPE_UP
ACTION_SWIPE_DOWN = com.theustech.blindcheck.ACTION_SWIPE_DOWN
ACTION_TTS_SMOKE = com.theustech.blindcheck.ACTION_TTS_SMOKE

BROADCAST = $(ADB) shell am broadcast -p $(TRACKING_PKG) -a

.PHONY: devices
devices:
	$(ADB) devices

## Builds + installs both apps, enables tracker + TTS capture, opens the test app, launches desktop remote
.PHONY: session
session: install-all enable-tts enable-tracker open-test-app desktop

.PHONY: resume-session
resume-session: enable-tts enable-tracker open-test-app desktop

## Builds + installs both apps, enables tracker + TTS capture, opens the test app, without desktop
.PHONY: terminal-session
terminal-session: install-all enable-tts enable-tracker open-test-app

.PHONY: resume-terminal-session
resume-terminal-session: enable-tts enable-tracker open-test-app

.PHONY: install-all
install-all:
	./gradlew :blindcheck-tracking-app:installDebug :blindcheck-test-app:installDebug

.PHONY: enable-tracker
enable-tracker:
	@current_services="$$($(ADB) shell settings get secure enabled_accessibility_services | tr -d '\r')"; \
	current_enabled="$$($(ADB) shell settings get secure accessibility_enabled | tr -d '\r')"; \
	target_services="$(TALKBACK_SERVICE):$(TRACKING_SERVICE)"; \
	if [ "$$current_services" = "$$target_services" ] && [ "$$current_enabled" = "1" ]; then \
	  echo "Accessibility services already enabled."; \
	else \
	  $(ADB) shell settings put secure enabled_accessibility_services null; \
	  $(ADB) shell settings put secure accessibility_enabled 0; \
	  $(ADB) shell settings put secure enabled_accessibility_services "$$target_services"; \
	  $(ADB) shell settings put secure accessibility_enabled 1; \
	fi
	@echo "Waiting for accessibility services to connect..."
	@for i in 1 2 3 4 5 6 7 8 9 10; do \
	  result=$$($(ADB) shell settings get secure enabled_accessibility_services 2>/dev/null); \
	  echo "  [$$i/10] $$result"; \
	  echo "$$result" | grep -q "blindcheck" && echo "  Tracker service ready." && exit 0; \
	  sleep 1; \
	done; echo "  WARNING: tracker service did not appear within 10s."

.PHONY: enable-tts
enable-tts:
	@current_engine="$$($(ADB) shell settings get secure tts_default_synth | tr -d '\r')"; \
	changed=0; \
	if [ "$$current_engine" = "$(TTS_ENGINE)" ]; then \
	  echo "TTS engine already set to $(TTS_ENGINE)"; \
	else \
	  $(ADB) shell settings put secure tts_default_synth $(TTS_ENGINE); \
	  echo "TTS engine set to $(TTS_ENGINE)"; \
	  changed=1; \
	fi; \
	plugins="$$($(ADB) shell settings get secure tts_enabled_plugins | tr -d '\r')"; \
	case ":$$plugins:" in \
	  *:$(TTS_ENGINE):*) echo "TTS plugin already enabled: $(TTS_ENGINE)" ;; \
	  :|:null:) $(ADB) shell settings put secure tts_enabled_plugins $(TTS_ENGINE) ;; \
	  *) $(ADB) shell settings put secure tts_enabled_plugins "$$plugins:$(TTS_ENGINE)" ;; \
	esac; \
	echo "Current TTS engine: $$($(ADB) shell settings get secure tts_default_synth)"; \
	if [ "$$changed" = "1" ]; then \
	  services="$$($(ADB) shell settings get secure enabled_accessibility_services | tr -d '\r')"; \
	  enabled="$$($(ADB) shell settings get secure accessibility_enabled | tr -d '\r')"; \
	  if [ "$$services" = "$(TALKBACK_SERVICE):$(TRACKING_SERVICE)" ] && [ "$$enabled" = "1" ]; then \
	    echo "Restarting accessibility services after TTS engine change..."; \
	    $(ADB) shell settings put secure enabled_accessibility_services null; \
	    $(ADB) shell settings put secure accessibility_enabled 0; \
	    $(ADB) shell am force-stop $(TALKBACK_PKG) || true; \
	    sleep 1; \
	    $(ADB) shell settings put secure enabled_accessibility_services "$(TALKBACK_SERVICE):$(TRACKING_SERVICE)"; \
	    $(ADB) shell settings put secure accessibility_enabled 1; \
	  fi; \
	fi

.PHONY: open-test-app
open-test-app:
	$(ADB) shell monkey -p $(TEST_APP_PKG) -c android.intent.category.LAUNCHER 1

.PHONY: desktop
desktop:
	@if pgrep -f "com.theustech.blindcheck_desktop.MainKt" >/dev/null; then \
	  echo "blindcheck-desktop already running."; \
	else \
	  ./gradlew :blindcheck-desktop:run & \
	fi

.PHONY: restart-desktop
restart-desktop:
	@pkill -f "com.theustech.blindcheck_desktop.MainKt" 2>/dev/null || true
	@sleep 1
	./gradlew :blindcheck-desktop:run &

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

## ── Remote navigation (terminal validation) ─────────────────────────────────
.PHONY: next previous activate back scroll-forward scroll-backward home recents swipe-up swipe-down

next:
	$(BROADCAST) $(ACTION_NEXT)

previous:
	$(BROADCAST) $(ACTION_PREVIOUS)

activate:
	$(BROADCAST) $(ACTION_ACTIVATE)

back:
	$(BROADCAST) $(ACTION_BACK)

scroll-forward:
	$(BROADCAST) $(ACTION_SCROLL_FORWARD)

scroll-backward:
	$(BROADCAST) $(ACTION_SCROLL_BACKWARD)

home:
	$(BROADCAST) $(ACTION_HOME)

recents:
	$(BROADCAST) $(ACTION_RECENTS)

swipe-up:
	$(BROADCAST) $(ACTION_SWIPE_UP)

swipe-down:
	$(BROADCAST) $(ACTION_SWIPE_DOWN)

.PHONY: tts-smoke
tts-smoke:
	$(BROADCAST) $(ACTION_TTS_SMOKE) --es com.theustech.blindcheck.EXTRA_TTS_TEXT BlindCheck_TTS_smoke_test

.PHONY: tts-smoke-explicit
tts-smoke-explicit:
	$(BROADCAST) $(ACTION_TTS_SMOKE) --es com.theustech.blindcheck.EXTRA_TTS_TEXT BlindCheck_TTS_smoke_test --es com.theustech.blindcheck.EXTRA_TTS_ENGINE_PACKAGE $(TTS_ENGINE)

.PHONY: navgraph-tts-matrix
navgraph-tts-matrix:
	./scripts/run-navgraph-tts-matrix.sh

## Watch accessibility event logs (BlindCheckRemote + BlindCheckTracker tags)
.PHONY: logs
logs:
	$(ADB) logcat -s BlindCheckRemote:D BlindCheckTracker:D BlindCheckAnnounce:I

## ── Tests ────────────────────────────────────────────────────────────────────
.PHONY: test
test:
	./gradlew connectedDebugAndroidTest
