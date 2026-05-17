ADB=adb
TRACKING_PKG=com.theustech.blindcheck_tracking_app
TRACKING_SERVICE=$(TRACKING_PKG)/.TrackingAccessibilityService
TALKBACK_SERVICE=com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
TEST_APP_PKG=com.theustech.blindcheck_testeapp

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
	@$(ADB) shell sh -c 'for i in 1 2 3 4 5 6 7 8 9 10; do \
	  val=$$(settings get secure enabled_accessibility_services); \
	  echo "  [$${i}/10] $$val"; \
	  echo "$$val" | grep -q "blindcheck" && echo "  Tracker service ready." && exit 0; \
	  sleep 1; \
	done; echo "  WARNING: tracker service did not appear in time."'

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

.PHONY: test
test:
	./gradlew connectedDebugAndroidTest
