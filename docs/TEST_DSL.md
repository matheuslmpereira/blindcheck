# Test DSL

## Goal

Provide a readable Kotlin DSL for testing blind-user accessibility flows using a cohesive user-action contract.

The DSL should support agentic code generation: small tests, clear expected behavior, deterministic data, and validation-friendly output.

## Example

```kotlin
blindCheck("login flow") {
    targetPackage("com.theustech.tts_test")

    setup {
        enableTracker()
        clearEvents()
    }

    launchApp()

    next()
    expectFocused {
        textContains("Acessar conta")
    }

    next()
    expectFocused {
        textContains("E-mail")
        editable()
    }
    inputText("matheus@email.com")

    next()
    expectFocused {
        textContains("Senha")
        editable()
    }
    inputText("123456")

    next()
    expectFocused {
        textContains("Entrar")
        clickable()
    }
    activate()

    expectScreen {
        textContains("Frutas")
    }
}
```

## Error flow example

```kotlin
blindCheck("login error flow") {
    targetPackage("com.theustech.tts_test")

    setup {
        enableTracker()
        clearEvents()
    }

    launchApp()

    navigateTo("Entrar")
    activate()

    expectFeedbackSequence {
        contains("Informe o e-mail")
        contains("Informe a senha")
    }
}
```

## Core actions

```kotlin
next()
previous()
activate()
scrollForward()
scrollBackward()
inputText("value")
back()
```

These actions represent user intent. The implementation may use node actions, global actions, accessibility focus, or dispatched gestures.

## Core assertions

```kotlin
expectFocused {
    textContains("Entrar")
    clickable()
    enabled()
}
```

```kotlin
expectFocusSequence {
    item("Acessar conta")
    item("E-mail")
    item("Senha")
    item("Entrar")
}
```

```kotlin
expectError {
    field("E-mail")
    message("Informe o e-mail")
    reachableByScreenReader()
}
```

## Assertion types

### Node assertions

* `textEquals`
* `textContains`
* `contentDescriptionEquals`
* `contentDescriptionContains`
* `viewIdResourceNameEquals`
* `clickable`
* `editable`
* `enabled`
* `disabled`
* `selected`
* `checked`
* `hasAction`

### Flow assertions

* `expectFocused`
* `expectFocusSequence`
* `expectFeedbackSequence`
* `expectScreenChanged`
* `expectScrollableReachedEnd`

### Error assertions

* `expectError`
* `reachableByScreenReader`
* `associatedWithField`

## TDD guidance

When adding a DSL feature:

1. create a failing test using the intended DSL;
2. implement the smallest action/assertion behavior;
3. run the relevant unit or instrumented test;
4. update this document if the public API changes.

## Important naming rule

Avoid naming assertions as if they guarantee exact TalkBack speech unless the TTS spy is active.

Prefer:

```kotlin
expectFeedbackSequence { }
```

Over:

```kotlin
expectTalkBackSpeech { }
```

Unless a real TTS spy is active.
