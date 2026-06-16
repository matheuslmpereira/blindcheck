# Tracking App Specification

## Goal

The tracking app provides observability for accessibility events during development and testing.

It is also the bridge that enables the `:blindcheck-testing` to drive user-level accessibility actions through an `AccessibilityService`.

It lets visual developers inspect the accessibility experience without relying only on manual TalkBack listening.

## Main features

### User action bridge

Expose the runtime capabilities needed by `UserAccessibilityActions`:

* move to next accessible element;
* move to previous accessible element;
* activate current/found node;
* scroll forward;
* scroll backward;
* input text;
* perform back action.

The app may implement these through accessibility node actions, global actions, accessibility focus, and `dispatchGesture()`.

### Event stream

Show a live list of captured accessibility events.

Each event should show:

* timestamp;
* package name;
* event type;
* text;
* content description;
* class name;
* focused/clickable/editable state when available.

### TTS speech capture

Expose an opt-in `TextToSpeechService` engine for controlled emulator/device runs.

When the BlindCheck TTS engine is selected as the system text-to-speech output, it should:

* capture each `SynthesisRequest` text;
* keep a short in-memory speech log;
* show the latest captured speech in the tracking app;
* emit `TTS <text>` to the same `BlindCheckAnnounce` logcat channel used by the desktop app;
* return silent PCM audio so bots can observe requested speech without depending on device audio.

This captures text sent to the selected TTS engine. It does not capture TalkBack output when another TTS engine is active.

### Inferred earcon feedback

Some TalkBack feedback is a short sound instead of TTS speech.

For remote `next` and `previous` actions, the tracker should infer navigation-boundary feedback when no new accessibility focus event arrives within a short timeout. The log format should be:

```text
EARCON boundary-next
EARCON boundary-previous
```

This is an inferred bot-friendly signal, not raw audio capture.

### Package filter

Allow filtering events by target app package.

Example:

```text
com.theustech.tts_test
```

### Event details

When selecting an event, show:

* event metadata;
* source node snapshot;
* parent/children nodes when available;
* node actions;
* bounds in screen.

### Current tree

Show the current active window accessibility tree.

This helps identify:

* missing accessible nodes;
* noisy nodes;
* unexpected focusable elements;
* duplicated labels.

### Flow recording

Controls:

* start recording;
* stop recording;
* clear recording;
* export JSON.

Recorded flows should be usable by the test library and readable by humans during pull-request review.

### Export

Export format should be stable and versioned.

```json
{
  "schemaVersion": "blindcheck-flow-v1",
  "targetPackage": "com.theustech.tts_test",
  "startedAt": 1770000000000,
  "events": []
}
```

## MVP UI

```text
Screen: Event Stream
- package filter input
- start/stop recording button
- clear button
- export button
- event list
```

A later debug panel may expose manual action buttons for `next`, `previous`, `activate`, `scrollForward`, `scrollBackward`, `inputText`, and `back`.

## Storage

MVP can use in-memory storage first.

Next step:

* Room database;
* file export;
* share intent.

## Validation

For each :blindcheck-tracking-app feature, prefer tests around:

* event normalization;
* package filtering;
* export serialization;
* action command handling;
* failure handling when node/source is null.

## Limitations

The tracking app observes accessibility events. It does not automatically know whether a blind user understood the experience.

Human review and test assertions are still needed.
