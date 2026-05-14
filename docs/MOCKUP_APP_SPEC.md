# Mockup Test App Specification

## Goal

Provide a controlled app used to validate BlindCheck features.

The mockup app must be deterministic because it is the reference target for agent-generated code, TDD, and end-to-end accessibility-flow validation.

## Screens

### Login

Elements:

* title: `Acessar conta`;
* email field;
* password field;
* submit button: `Entrar`.

Behaviors:

* empty submit shows email and password errors;
* valid submit navigates to fruit list.

Broken cases:

* visual error with missing accessible feedback;
* unlabeled button;
* duplicate label;
* noisy decorative element.

### Fruit List

Elements:

* title: `Frutas`;
* `LazyColumn` with many items;
* each fruit item is clickable.

Behaviors:

* selecting a fruit opens detail screen;
* list must support scroll navigation.

Broken cases:

* item without useful label;
* overly verbose item;
* scrollable container with bad focus sequence.

### Fruit Detail

Elements:

* back button;
* fruit image;
* fruit title;
* fruit description.

Behaviors:

* back returns to list.

Broken cases:

* decorative image exposed as meaningful;
* meaningful image missing description;
* back button without label.

## Test flows

Each flow should have an expected accessibility sequence documented in tests.

### Happy path

```text
Login → Fruit List → Fruit Detail → Back
```

### Error path

```text
Login → Submit empty → Error feedback
```

Expected result:

* the error feedback is observable in the accessibility event stream;
* the relevant field/error text can be asserted by the test library;
* intentionally broken cases must fail the relevant assertion.

### Long list path

```text
Login → Fruit List → Scroll → Select offscreen item
```

## Development rules

* Keep data deterministic.
* Avoid network dependencies.
* Avoid flaky animations in test paths.
* Keep text stable unless tests/docs are updated.
* Add broken cases deliberately and document the expected failure.
