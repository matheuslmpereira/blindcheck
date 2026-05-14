# Project Scope

## Focus

BlindCheck focuses on accessibility for blind users on Android.

The core question is:

> Can a blind user understand, navigate, and complete a flow using accessibility navigation?

BlindCheck does not attempt to perfectly emulate TalkBack speech. It validates the observable accessibility layer that TalkBack consumes and the sequence of user-level accessibility actions.

## In scope

### User actions

BlindCheck should expose a cohesive user-action contract:

* next;
* previous;
* activate;
* scroll forward;
* scroll backward;
* input text;
* back navigation.

The implementation details are internal.

### Accessibility feedback

BlindCheck observes and validates:

* focused node text;
* content description;
* role/class;
* enabled/disabled state;
* selected state;
* checked state;
* editable state;
* available actions;
* error messages.

### Flow validation

BlindCheck validates:

* expected focus sequence;
* missing accessible elements;
* unexpected noisy elements;
* inaccessible visual errors when observable in the accessibility layer;
* flow export and replay;
* feedback sequence.

## Out of scope for MVP

* visual contrast validation;
* pixel-based rendering checks;
* low-vision UX;
* font scaling;
* color-blindness validation;
* complete WCAG scoring;
* iOS support;
* Android Studio plugin;
* exact TalkBack speech guarantee.

## Success criteria

The MVP succeeds when a developer can:

1. run the mockup app;
2. install the tracking app;
3. enable the accessibility service;
4. drive user-level accessibility actions through the test library;
5. record accessibility events;
6. assert the expected focus and feedback sequence in a test;
7. export the flow for review/debugging.

## Agentic delivery requirement

Every implementation delivery should include:

```text
Validated:
- command run:
- result:
- tests added/updated:
- known limitations:
```

If a command cannot be run, the reason must be stated explicitly.
