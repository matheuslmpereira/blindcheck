# Architecture

## Overview

BlindCheck is composed of three main parts:

```text
:blindcheck-test-app
    ↓ exposes deterministic accessibility flows
:blindcheck-tracking-app
    ↓ observes AccessibilityEvent / AccessibilityNodeInfo data
    ↓ executes user-level accessibility actions through AccessibilityService
:blindcheck-testing
    ↓ drives actions and asserts expected behavior
instrumented tests / CI
```

The main abstraction is the user-action contract.

The implementation may use `AccessibilityNodeInfo.performAction()`, accessibility focus actions, global actions, or `dispatchGesture()`, but consumers should use one cohesive API.

## Modules

### :blindcheck-testing

The testing API used by developers.

Responsibilities:

* setup accessibility services via ADB;
* expose `UserAccessibilityActions`;
* trigger user-level accessibility actions;
* read observed or exported events;
* assert expected focus sequence;
* assert accessible feedback;
* provide a Kotlin DSL for tests.

The library should avoid parallel public concepts such as separate gesture and semantic navigators. Those are implementation strategies, not product-level APIs.

### :blindcheck-tracking-app

The observability and action bridge layer.

Responsibilities:

* run an `AccessibilityService`;
* listen to `AccessibilityEvent`;
* inspect `AccessibilityNodeInfo`;
* normalize events;
* store events locally;
* filter events by target package;
* export flows as JSON;
* execute user-level accessibility actions requested by the test library.

### :blindcheck-test-app

The validation playground.

Responsibilities:

* provide stable test screens;
* simulate real Compose flows;
* include intentionally broken accessibility states;
* validate BlindCheck itself.

## Data flow

```text
Test calls UserAccessibilityActions
    ↓
AccessibilityService performs action or gesture
    ↓
Target app UI changes
    ↓
Android emits AccessibilityEvent
    ↓
:blindcheck-tracking-app captures event
    ↓
:blindcheck-tracking-app stores normalized record
    ↓
:blindcheck-testing reads observed/exported records
    ↓
assertions validate expected behavior
```

For manual debugging, a developer may also navigate manually while the tracking app records the same event stream.

## Core entities

```kotlin
data class A11yEventRecord(
    val id: String,
    val timestamp: Long,
    val packageName: String?,
    val eventType: String,
    val className: String?,
    val text: List<String>,
    val contentDescription: String?,
    val sourceNode: A11yNodeSnapshot?
)
```

```kotlin
data class A11yNodeSnapshot(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val viewIdResourceName: String?,
    val packageName: String?,
    val clickable: Boolean,
    val enabled: Boolean,
    val focused: Boolean,
    val selected: Boolean,
    val editable: Boolean,
    val actions: List<String>,
    val boundsInScreen: RectSnapshot,
    val children: List<A11yNodeSnapshot>
)
```

```kotlin
data class RectSnapshot(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
```

## Design principles

* Prefer observable accessibility behavior over visual assumptions.
* Keep the first version Android-only.
* Expose one cohesive user-action contract.
* Treat TalkBack output as approximated unless captured through a controlled TTS engine.
* Make exported flows human-readable and versionable.
* Keep the testing DSL expressive but deterministic.
* Use TDD whenever behavior can be validated with unit or instrumented tests.
* Every implementation should be validated with the most relevant Gradle/test command.
