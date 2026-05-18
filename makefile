ADB=adb
TRACKING_PKG=com.theustech.blindcheck_tracking_app
TRACKING_SERVICE=$(TRACKING_PKG)/com.theustech.blindcheck_interactor.TrackingAccessibilityService
TALKBACK_SERVICE=com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
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

BROADCAST = $(ADB) shell am broadcast -p $(TRACKING_PKG) -a

.PHONY: devices
devices:
	$(ADB) devices

## Builds + installs both apps, enables the tracker service, opens the test app, launches desktop remote
.PHONY: session
session: install-all enable-tracker open-test-app desktop

.PHONY: install-all
install-all:
	./gradlew :blindcheck-tracking-app:installDebug :blindcheck-test-app:installDebug

.PHONY: enable-tracker
enable-tracker:
	$(ADB) shell settings put secure enabled_accessibility_services $(TALKBACK_SERVICE):$(TRACKING_SERVICE)
	$(ADB) shell settings put secure accessibility_enabled 1
	@echo "Waiting for accessibility services to connect..."
	@for i in 1 2 3 4 5 6 7 8 9 10; do \
	  result=$$($(ADB) shell settings get secure enabled_accessibility_services 2>/dev/null); \
	  echo "  [$$i/10] $$result"; \
	  echo "$$result" | grep -q "blindcheck" && echo "  Tracker service ready." && exit 0; \
	  sleep 1; \
	done; echo "  WARNING: tracker service did not appear within 10s."

.PHONY: open-test-app
open-test-app:
	$(ADB) shell monkey -p $(TEST_APP_PKG) -c android.intent.category.LAUNCHER 1

.PHONY: desktop
desktop:
	./gradlew :blindcheck-desktop:run &

.PHONY: disable-a11y
disable-a11y:
	$(ADB) shell settings put secure enabled_accessibility_services null
	$(ADB) shell settings put secure accessibility_enabled 0

.PHONY: check-a11y
check-a11y:
	$(ADB) shell settings get secure enabled_accessibility_services
	$(ADB) shell settings get secure accessibility_enabled

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

## Watch accessibility event logs (BlindCheckRemote + BlindCheckTracker tags)
.PHONY: logs
logs:
	$(ADB) logcat -s BlindCheckRemote:D BlindCheckTracker:D

## ── Tests ────────────────────────────────────────────────────────────────────
.PHONY: test
test:
	./gradlew connectedDebugAndroidTest
