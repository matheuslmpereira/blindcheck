# BlindCheck

BlindCheck is an Android accessibility testing toolkit focused on the experience of blind users.

It helps developers observe, drive, test, and version the accessibility layer consumed by screen readers in Android apps, including apps built with Jetpack Compose.

The internal project name is `accessibility-validations`.

## Problem

Testing Android accessibility for blind users is still difficult for visual developers.

Common problems:

* TalkBack navigation is slow and tiring for developers who are not used to it.
* Existing automated tools miss important screen-reader UX issues.
* Compose accessibility checks do not fully represent the real navigation experience.
* Accessibility regressions are hard to review in pull requests.
* Teams lack a practical way to observe, record, drive, and assert accessibility flows.

## Goal

BlindCheck aims to make blind-user accessibility testable, observable, and reviewable during development.

BlindCheck validates the observable accessibility layer:

* `AccessibilityEvent` stream;
* `AccessibilityNodeInfo` snapshots;
* focus sequence;
* node text;
* content descriptions;
* states;
* available actions;
* user actions executed through an accessibility service.

BlindCheck can drive user-level accessibility actions through a companion `AccessibilityService`, observe the resulting accessibility events, and assert expected navigation/feedback flows.

## What BlindCheck does not claim

BlindCheck does not guarantee universal exact TalkBack speech output.

TalkBack applies its own interpretation based on Android version, TalkBack version, locale, verbosity settings, navigation mode, and user preferences.

BlindCheck focuses on the accessibility layer that TalkBack consumes and the actions a user would perform.

## First version focus

The first version focuses on:

* screen-reader navigation flow;
* focus sequence;
* accessible actions;
* accessible feedback;
* error discoverability;
* event tracking;
* flow export for test and review.

## Non-goals for the first version

The first version does not focus on:

* visual contrast;
* low-vision support;
* color blindness;
* font scaling;
* touch-target validation;
* general WCAG compliance scoring;
* exact TalkBack speech capture.

Those may become separate modules later.

## Modules

```text
accessibility-validations/
├── :blindcheck-testing/
├── :blindcheck-tracking-app/
├── :blindcheck-test-app/
└── docs/
```

### :blindcheck-testing

A Kotlin/Android testing library that provides:

* ADB helpers for accessibility setup;
* a single user-action contract for accessibility navigation;
* assertions for focus sequence and accessible feedback;
* helpers to consume observed/exported accessibility flows;
* a DSL for blind-user accessibility tests.

The public API should describe user intent, not internal implementation mode.

Internally, actions may use:

* `AccessibilityNodeInfo.performAction()`;
* `ACTION_ACCESSIBILITY_FOCUS`;
* `AccessibilityService.performGlobalAction()`;
* `AccessibilityService.dispatchGesture()`.

### :blindcheck-tracking-app

A companion Android app with an `AccessibilityService` used to observe and drive accessibility flows.

It provides:

* event stream;
* package filter;
* current accessibility node snapshot;
* user-action bridge;
* flow recording;
* JSON export.

### :blindcheck-test-app

A sample Android app used to validate the toolkit.

It contains:

* login screen;
* validation errors;
* fruit list using `LazyColumn`;
* fruit detail screen;
* intentionally broken accessibility cases.

## Development philosophy

BlindCheck is an accessibility tooling project. Broken tooling can create false confidence.

Use a TDD-oriented workflow whenever possible:

```text
1. Write or update the failing test first.
2. Implement the smallest code change that makes the test pass.
3. Run the relevant Gradle task.
4. Refactor only after the test is green.
5. Update docs if behavior, architecture, or public API changed.
```

Every meaningful code delivery should include a validation summary.
