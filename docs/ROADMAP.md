# Roadmap

## MVP 1 — Vertical accessibility-flow slice

Features:

* mockup app with deterministic login and fruit-list flows;
* tracking app with `AccessibilityService`;
* capture accessibility events;
* filter by package;
* expose `UserAccessibilityActions` in the test library;
* implement `next`, `previous`, `activate`, `scrollForward`, `scrollBackward`, `inputText`, and `back`;
* assert focused node and accessible feedback;
* show event list.

Success criteria:

* test library can drive the mockup app without visual clicks;
* tracking app captures the resulting event stream;
* login focus sequence can be tested;
* login error feedback can be tested.

## MVP 2 — Flow export and replay

Features:

* export flow as JSON;
* load exported JSON;
* assert focus sequence from exported flows;
* assert feedback sequence from exported flows;
* stable schema versioning.

Success criteria:

* exported JSON includes focus/click/window/scroll/input events;
* exported flow is readable enough for manual review;
* exported flow can be used by :blindcheck-testing assertions.

## MVP 3 — ADB setup helpers

Features:

* enable tracking service;
* disable accessibility services;
* check current accessibility settings;
* open accessibility settings.

Success criteria:

* developer can setup the emulator with one command or receive a clear fallback instruction.

## MVP 4 — TTS spy

Features:

* fake TTS engine; initial engine implemented in `:blindcheck-tracking-app`;
* capture synthesized utterances in memory and logcat;
* export speech log through the tracking app dump;
* compare expected approximate speech.

Success criteria:

* developer can inspect the text sent to TTS in a controlled emulator when BlindCheck is selected as the system TTS engine.

## Future — Android Studio plugin

Features:

* actions for next/previous/activate/scroll;
* event stream panel;
* export current flow;
* highlight current accessibility node.

Success criteria:

* developer can inspect and navigate accessibility flows directly from Android Studio.

## Agentic development roadmap rule

Each roadmap item should be implemented as a small, validated vertical slice.

Preferred delivery format:

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
