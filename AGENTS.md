# AGENTS.md

This file provides guidance for coding agents working on this repository.

Project name: `accessibility-validations`
Public/product name: `BlindCheck`

BlindCheck is an Android accessibility tooling project focused on helping developers test, observe, and validate the experience of blind users navigating Android apps.

---

## 1. Product intent

BlindCheck exists because testing accessibility for blind users is difficult, slow, and poorly supported by existing Android tooling.

The project aims to make screen-reader-oriented accessibility:

* observable during development;
* testable in automated and semi-automated flows;
* reviewable in pull requests;
* exportable as structured data;
* easier for visual developers to understand.

The project is not a generic WCAG validator in its first version.

The core question is:

> Can a blind user understand, navigate, and complete this flow using accessibility navigation?

---

## 2. Required reading before implementation

Before making architectural or feature changes, read the relevant docs in this order:

1. `docs/PROJECT_SCOPE.md`
2. `docs/ARCHITECTURE.md`
3. `docs/NAVIGATION_MODEL.md`
4. `docs/TRACKING_APP_SPEC.md`
5. `docs/TEST_DSL.md`
6. `docs/MOCKUP_APP_SPEC.md`
7. `docs/LIMITATIONS.md`
8. `docs/ROADMAP.md`

When implementing a task, align the code with these documents. If implementation requires changing scope or architecture, update the docs in the same pull request.

---

## 3. Main modules

The repository is organized around three main modules.

```text
accessibility-validations/
├── :blindcheck-testing/
├── :blindcheck-tracking-app/
├── :blindcheck-test-app/
└── docs/
```

### 3.1 `:blindcheck-testing`

Purpose: provide APIs for writing accessibility-flow tests.

Responsibilities:

* ADB helpers for accessibility setup;
* abstraction of accessibility navigation actions;
* assertions for focus sequence;
* assertions for accessible feedback;
* helpers to consume exported flows from the tracking app;
* future DSL for blind-user navigation flows.

Expected direction:

```kotlin
blindCheck("login flow") {
    targetPackage("com.example.app")

    setup {
        enableTracker()
        clearEvents()
    }

    launchApp()

    expectFocusSequence {
        item { textContains("Acessar conta") }
        item { textContains("E-mail"); editable() }
        item { textContains("Senha"); editable() }
        item { textContains("Entrar"); clickable() }
    }
}
```

### 3.2 `:blindcheck-tracking-app`

Purpose: observe accessibility events in debug/development.

Responsibilities:

* implement an `AccessibilityService`;
* listen to `AccessibilityEvent`;
* inspect `AccessibilityNodeInfo` when available;
* normalize events into stable data models;
* filter events by target package name;
* display event stream;
* export flows as JSON.

The tracking app is not the app under test. It is a companion observability tool.

### 3.3 `:blindcheck-test-app`

Purpose: provide a controlled app for validating BlindCheck itself.

Responsibilities:

* login screen;
* login error states;
* fruit list using `LazyColumn`;
* fruit detail screen;
* intentionally broken accessibility cases.

The mockup app should remain simple, deterministic, and easy to reason about.

---

## 4. Current MVP focus

Prioritize the MVP as one vertical slice:

1. `:blindcheck-test-app` with deterministic login and fruit-list flows.
2. `:blindcheck-tracking-app` with `AccessibilityService`.
3. Event capture and filtering by package name.
4. A user-action contract exposed by `:blindcheck-testing`.
5. Navigation/action execution through the accessibility service.
6. Assertions for expected focus and accessible feedback sequence.
7. JSON export of accessibility flows.

The test library should expose a single user-action contract. It should not expose separate product concepts such as "semantic navigation mode" versus "gesture navigation mode" to consumers.

Internally, an action may be implemented with `AccessibilityNodeInfo.performAction()`, `AccessibilityService.performGlobalAction()`, `ACTION_ACCESSIBILITY_FOCUS`, or `dispatchGesture()`, depending on which implementation is the best fit.

Do not start with the Android Studio plugin, TTS spy, or visual-accessibility checks until the core user-action contract and tracking flow are working.

---

## 5. In scope

BlindCheck currently focuses on accessibility for blind users.

In-scope areas:

* accessibility focus sequence;
* screen-reader navigation path;
* accessible labels;
* content descriptions;
* editable fields;
* clickable actions;
* enabled/disabled state;
* selected/checked state;
* accessible error feedback;
* scrollability from an accessibility perspective;
* event recording and flow export;
* developer observability of accessibility events.

---

## 6. Out of scope for MVP

Do not prioritize these areas in the first version:

* visual contrast validation;
* pixel-based rendering checks;
* low-vision support;
* color blindness;
* font scaling;
* touch-target validation;
* complete WCAG compliance scoring;
* iOS support;
* Android Studio plugin;
* exact TalkBack speech guarantee.

These can become future modules, but they should not distract from the blind-user navigation MVP.

---

## 7. Important terminology

Use precise names.

### Preferred terms

* `accessibility event`
* `accessibility node`
* `focus sequence`
* `accessible feedback`
* `screen-reader navigation`
* `blind-user flow`
* `recorded flow`
* `exported flow`
* `approximate speech`

### Avoid unless technically true

Avoid claiming exact TalkBack behavior unless the feature is actually capturing TalkBack/TTS output in a controlled environment.

Do not name APIs as if they guarantee exact TalkBack speech when they only infer feedback from accessibility events.

Prefer:

```kotlin
expectFeedbackSequence { }
```

Instead of:

```kotlin
expectTalkBackSpeech { }
```

Unless a real TTS spy is active.

---

## 8. Accessibility model

BlindCheck should distinguish clearly between these layers:

```text
Visual UI
↓
Compose Semantics Tree
↓
Android Accessibility Tree
↓
AccessibilityEvent stream
↓
TalkBack interpretation
↓
TTS output
```

The MVP controls and observes:

```text
Android Accessibility Tree
AccessibilityEvent stream
User actions executed by an AccessibilityService
```

The MVP should provide a contract for user actions such as:

```kotlin
interface UserAccessibilityActions {
    suspend fun next()
    suspend fun previous()
    suspend fun activate()
    suspend fun scrollForward()
    suspend fun scrollBackward()
    suspend fun inputText(value: String)
    suspend fun back()
}
```

The public contract should describe user intent. Implementation details may use node actions, global actions, accessibility focus actions, or dispatched gestures.

The MVP should not pretend to fully reproduce TalkBack speech internals.

---

## 9. Event model guidelines

Use stable, serializable models for recorded events.

Recommended base model:

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

Recommended node snapshot model:

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

Recommended rectangle model:

```kotlin
data class RectSnapshot(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
```

Avoid leaking Android framework classes like `AccessibilityNodeInfo`, `Rect`, or `AccessibilityEvent` into exported JSON models.

---

## 10. Export format guidelines

Exported flows must be versioned.

Recommended initial schema:

```json
{
  "schemaVersion": "blindcheck-flow-v1",
  "targetPackage": "com.example.app",
  "startedAt": 1770000000000,
  "endedAt": 1770000005000,
  "events": []
}
```

Rules:

* keep exported JSON human-readable;
* preserve event order;
* include timestamps;
* include target package;
* include schema version;
* avoid unstable object dumps;
* avoid storing unnecessary sensitive user input by default.

For text input events, consider redaction options.

---

## 11. Privacy and sensitive data

Accessibility events can expose sensitive information.

Treat exported flows as potentially sensitive.

Rules:

* do not log passwords;
* provide redaction for editable text;
* avoid exporting user-entered values unless explicitly enabled;
* document that exported flows may contain UI text and user input;
* prefer package-filtered capture instead of global capture;
* keep debug tooling explicit and opt-in.

The mockup app may use fake data only.

---

## 12. Testing strategy

BlindCheck is a tooling project. Code changes must be validated with more discipline than a normal sample app because broken tooling can create false confidence about accessibility.

Use a TDD-oriented workflow whenever the task can be expressed as observable behavior.

### Preferred development loop

```text
1. Write or update the failing test first.
2. Implement the smallest code change that makes the test pass.
3. Run the relevant Gradle task.
4. Refactor only after the test is green.
5. Update docs if behavior, architecture, or public API changed.
```

### Unit tests

Use unit tests for:

* event normalization;
* `AccessibilityEvent` to `A11yEventRecord` mapping;
* `AccessibilityNodeInfo` to `A11yNodeSnapshot` mapping;
* JSON serialization/deserialization;
* assertion matching;
* DSL execution logic;
* package filtering;
* event redaction;
* flow comparison.

### Instrumented tests

Use instrumented tests for:

* `AccessibilityService` registration and behavior;
* event capture from the mockup app;
* user-action execution through `UserAccessibilityActions`;
* focus movement;
* activation;
* input;
* scroll;
* end-to-end recording and assertion flows.

### Mockup app tests

Use the mockup app to verify:

* login happy path;
* login error path;
* long list navigation;
* fruit detail navigation;
* intentionally broken accessibility cases.

### Validation requirement

Every implementation task should end with a short validation summary:

```text
Validated:
- command run:
- result:
- tests added/updated:
- known limitations:
```

If a test cannot be run, the agent must state why and provide the closest possible validation performed.

Do not rely only on Compose accessibility checks for this project. The goal is to observe Android accessibility behavior.

## 13. Coding standards

### Kotlin

* Prefer Kotlin idioms.
* Use immutable data classes for exported models.
* Keep side effects isolated.
* Prefer explicit names over abbreviations.
* Avoid premature abstraction.
* Keep MVP code understandable.

### Android

* Keep `AccessibilityService` code small and focused.
* Normalize Android framework objects into project models quickly.
* Do not pass framework objects across module boundaries unless necessary.
* Handle nullability defensively; accessibility APIs often return null.

### Compose

For the mockup app:

* keep screens simple;
* use stable test data;
* expose predictable labels;
* include broken cases intentionally and document them;
* prefer clear UI state models.

---

## 14. Implementation rules for agents

This repository is expected to be developed with coding agents such as Codex. Keep tasks small, verifiable, and cohesive.

When implementing features:

1. Read `AGENTS.md` and the relevant docs before changing code.
2. Prefer the smallest useful vertical slice.
3. Keep implementation aligned with the current MVP.
4. Use TDD whenever behavior can be expressed in a test.
5. Add or update tests before or alongside implementation.
6. Run the most relevant validation command before finishing.
7. Report what was validated and what was not.
8. Keep docs updated with behavior changes.
9. Do not introduce large frameworks unless clearly justified.
10. Avoid building the Android Studio plugin before the core tracking flow works.
11. Avoid adding visual accessibility checks to the MVP unless explicitly requested.
12. Do not claim exact TalkBack fidelity unless using a real capture mechanism.
13. Keep exported formats stable and versioned.
14. Add tests for model transformations and assertions.
15. Keep mockup app deterministic.
16. Preserve privacy defaults.

### Cohesion rules

The agent must preserve project cohesion.

Before adding a new class, module, dependency, or abstraction, check whether an existing concept already covers it.

Preferred cohesion model:

```text
UserAccessibilityActions
→ user-level action contract

A11yEventRecord
→ normalized event data

A11yNodeSnapshot
→ serializable node data

Flow assertions
→ validation of observed accessibility behavior
```

Avoid creating parallel abstractions with similar responsibilities.

Bad examples:

```text
GestureNavigator + SemanticNavigator + AccessibilityRunner
```

when the product contract should simply be:

```text
UserAccessibilityActions
```

Internal implementations may vary. Public concepts should remain stable and simple.

### Agentic delivery protocol

For every non-trivial task, the final agent response should include:

```text
Changed:
- files/modules changed

Validated:
- commands run
- tests passing/failing

Notes:
- assumptions
- limitations
- next safe step
```

Do not say a task is complete without compiling or explaining why compilation was not possible.

## 15. Expected first implementation slice

The first implementation slice should be a runnable vertical flow:

```text
:blindcheck-test-app
→ login flow
→ fruit list flow
→ deterministic accessibility labels and states
```

```text
:blindcheck-tracking-app
→ AccessibilityService
→ capture AccessibilityEvent
→ normalize event
→ show list of events
→ filter by package
```

```text
:blindcheck-testing
→ expose UserAccessibilityActions contract
→ execute next / previous / activate / scroll / input / back
→ observe resulting events
→ assert focus sequence
→ assert accessible feedback sequence
```

JSON export can be added once the in-memory flow is observable and assertable.

---

## 16. Suggested package naming

Use a stable namespace.

Suggested packages:

```text
br.com.theustech.blindcheck
br.com.theustech.blindcheck.tracker
br.com.theustech.blindcheck.testkit
br.com.theustech.blindcheck.sample
```

If the repository initially uses another namespace, keep package renames explicit and deliberate.

---

## 17. Error handling guidelines

Accessibility APIs are noisy and inconsistent.

Handle gracefully:

* null source node;
* empty text;
* empty content description;
* missing package name;
* inaccessible window content;
* duplicate events;
* rapid event bursts;
* stale nodes;
* unsupported actions.

Prefer structured error states over crashes in the tracking app.

For :blindcheck-testing assertions, fail with clear messages.

Example:

```text
Expected focus sequence item #3 to contain text "Entrar", but observed "Senha".
Target package: com.example.app
Event id: ...
Timestamp: ...
```

---

## 18. Documentation style

Docs should be:

* short;
* practical;
* implementation-oriented;
* explicit about limitations;
* updated with behavior changes.

Avoid marketing language inside technical docs except in `README.md`.

---

## 19. Definition of done

A feature is done when:

* code compiles;
* relevant unit tests or instrumented tests are added/updated;
* relevant tests pass, or failures are documented clearly;
* the implementation follows the existing module boundaries;
* the public API remains cohesive and aligned with the docs;
* docs are updated if behavior, architecture, or public API changed;
* exported models are stable or versioned;
* privacy implications are considered;
* failure modes are clear;
* the feature can be demonstrated with the mockup app when applicable.

For agent-generated code, also include a validation summary in the final response.

## 20. Strategic direction

BlindCheck should become a practical developer experience layer for blind-user accessibility testing on Android.

The long-term direction may include:

* Android Studio plugin;
* TTS spy;
* service-driven navigation runner;
* CI reports;
* flow snapshots;
* pull request accessibility diffs;
* optional Compose-specific assertions.

But the first priority is a runnable vertical slice that combines the mockup app, tracking service, user-action contract, and observable accessibility events.
