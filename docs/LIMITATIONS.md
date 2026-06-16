# Limitations

## TalkBack fidelity

BlindCheck does not guarantee universal exact TalkBack speech output in the first versions.

TalkBack applies its own heuristics based on:

* node text;
* content description;
* role;
* state;
* available actions;
* Android version;
* TalkBack version;
* user settings.

The MVP validates observable accessibility events, node snapshots, user-action results, and accessible feedback, not universal exact speech.

## TTS capture limitations

BlindCheck can capture exact text passed to Android text-to-speech only in controlled runs where the BlindCheck TTS engine is selected as the device text-to-speech output.

This is useful as a bot/reference log, but it has important limits:

* it captures `SynthesisRequest` text received by the BlindCheck engine, not arbitrary audio output;
* it will not see TalkBack speech if TalkBack is using Google TTS or another engine;
* it returns silent audio, so the feature is for logging and assertions, not for human listening;
* captured text may contain sensitive UI content and should be treated as sensitive test data.

## AccessibilityService limitations

Some actions depend on Android permissions and service capabilities.

Users may need to manually enable the service in settings depending on Android restrictions.

The public `UserAccessibilityActions` contract is stable, but individual action implementations may use different strategies internally depending on device/API behavior.

## Compose limitations

Compose semantics may merge, hide, or transform nodes.

BlindCheck should clearly distinguish:

* visual UI;
* Compose semantics tree;
* Android accessibility tree;
* captured accessibility events.

## Manual testing remains required

BlindCheck reduces manual testing cost, but does not fully replace real TalkBack testing with blind users or trained accessibility testers.

The goal is to reduce regressions and make the accessibility layer observable and testable during development.

## Agent-generated code limitations

Agent-generated code must be validated.

A generated implementation is not considered complete until the relevant build/test command was run or the reason it could not be run is documented.

For behavioral changes, prefer a TDD-oriented workflow: failing test first, minimal implementation, validation, then refactor.
