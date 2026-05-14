# Claude Review Briefing

This repository is `accessibility-validations`, public/product name `BlindCheck`.

BlindCheck is an Android accessibility tooling project focused on blind-user accessibility. The core question is:

> Can a blind user understand, navigate, and complete this flow using accessibility navigation?

## Product Boundaries

Prioritize the MVP vertical slice:

1. `:blindcheck-test-app` provides deterministic login and fruit-list flows.
2. `:blindcheck-tracking-app` provides an `AccessibilityService`, captures accessibility events, normalizes events, filters by package, and displays/export flows.
3. `:blindcheck-testing` exposes test APIs, including one cohesive `UserAccessibilityActions` contract and assertions for focus and accessible feedback.

Do not prioritize these in MVP reviews:

* visual contrast validation;
* pixel-based rendering checks;
* low-vision support;
* color blindness;
* font scaling;
* touch-target validation;
* complete WCAG scoring;
* iOS support;
* Android Studio plugin;
* TTS spy;
* exact TalkBack speech guarantees.

## Architecture Rules

* Keep public navigation/action API cohesive: `UserAccessibilityActions`.
* Do not introduce parallel public concepts such as `GestureNavigator`, `SemanticNavigator`, or separate navigation modes.
* Internal implementations may use `AccessibilityNodeInfo.performAction()`, `AccessibilityService.performGlobalAction()`, `ACTION_ACCESSIBILITY_FOCUS`, or `dispatchGesture()`.
* Normalize Android framework objects into stable project models quickly.
* Do not expose `AccessibilityNodeInfo`, `AccessibilityEvent`, or `Rect` through exported JSON models.
* Treat exported flows as sensitive because accessibility events may include user input.
* Do not claim exact TalkBack fidelity unless there is a controlled TTS capture mechanism.

## Expected Models And Contracts

Core concepts should stay aligned with:

* `UserAccessibilityActions`
* `A11yEventRecord`
* `A11yNodeSnapshot`
* `RectSnapshot`
* `BlindCheckFlow`
* focus sequence assertions
* accessible feedback assertions

Exported flows must remain versioned with `schemaVersion = "blindcheck-flow-v1"` when export is implemented.

## Review Priorities

When reviewing PRs, prioritize:

1. Regressions against blind-user accessibility intent.
2. Public API drift away from `UserAccessibilityActions`.
3. Incorrect leakage of Android framework classes into exported/shared models.
4. Missing null-safety around Android accessibility APIs.
5. Missing package filtering or privacy redaction where event capture/export is involved.
6. Tests that do not validate observable accessibility behavior.
7. PR scope creep into out-of-scope MVP areas.

For mockup-app changes, check that flows are deterministic, labels are stable, tests avoid network/flaky animations, and visible/accessibility-facing text remains predictable.

For tracking-app changes, check event normalization, null-safe node handling, package filtering, and failure behavior.

For `:blindcheck-testing` changes, check cohesive contracts, matcher clarity, and useful failure messages.

## Validation Expectations

Every implementation PR should compile or explicitly explain why compilation was not possible.

Prefer the most focused Gradle command first, then broader validation when practical:

* `./gradlew :blindcheck-testing:testDebugUnitTest`
* `./gradlew :blindcheck-test-app:testDebugUnitTest`
* `./gradlew :blindcheck-test-app:connectedDebugAndroidTest`
* `./gradlew :blindcheck-tracking-app:testDebugUnitTest`
* `./gradlew build`

Review feedback should be actionable. Use file/line references when possible, separate correctness issues from suggestions, and avoid requesting MVP-out-of-scope work.
