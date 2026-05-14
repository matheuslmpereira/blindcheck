ADB=adb
TRACKING_SERVICE=com.theustech.blindcheck.tracker/.TrackingAccessibilityService

.PHONY: devices
devices:
	$(ADB) devices

.PHONY: install-all
install-all:
	./gradlew :tracking-app:installDebug :mockup-test-app:installDebug

.PHONY: enable-tracker
enable-tracker:
	$(ADB) shell settings put secure enabled_accessibility_services $(TRACKING_SERVICE)
	$(ADB) shell settings put secure accessibility_enabled 1

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