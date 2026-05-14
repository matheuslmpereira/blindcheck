# Navigation Model

## Goal

Provide one abstraction that lets tests describe user-level accessibility actions without depending on visual UI interaction.

The public contract should express what the user intends to do. It should not expose whether the implementation used a node action, global action, accessibility focus action, or physical gesture.

## Core contract

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

## Action meanings

### next

Move to the next accessible element.

Equivalent user intent:

```text
Move screen-reader focus forward.
```

### previous

Move to the previous accessible element.

Equivalent user intent:

```text
Move screen-reader focus backward.
```

### activate

Activate the currently focused or matched element.

Equivalent user intent:

```text
Double tap with TalkBack.
```

### scrollForward

Scroll the current scrollable container forward.

### scrollBackward

Scroll the current scrollable container backward.

### inputText

Input text into the currently focused editable node.

### back

Trigger Android back navigation.

## Implementation strategy

For the MVP, implement the `UserAccessibilityActions` contract as a vertical slice.

The implementation may combine:

* `AccessibilityNodeInfo.performAction()`;
* `ACTION_ACCESSIBILITY_FOCUS`;
* `AccessibilityService.performGlobalAction()`;
* `AccessibilityService.dispatchGesture()`.

Tests and docs should use the public action names: `next`, `previous`, `activate`, `scrollForward`, `scrollBackward`, `inputText`, and `back`.

Manual recording remains useful for debugging and comparison, but the project goal is to provide a testable action contract.

## Limitations

The exact TalkBack focus algorithm may differ from BlindCheck's action implementation.

BlindCheck should document which implementation strategy was used internally when relevant, but the public contract should remain stable.

BlindCheck validates observable accessibility navigation and feedback. It does not guarantee universal exact TalkBack speech.
