# blindcheck-tracker

Biblioteca Android interna responsável por normalizar eventos do Android Accessibility Framework para modelos imutáveis e armazená-los em um store compartilhado.

**Não é uma dependência direta de quem quer testar apps.** Quem consome testes usa `:blindcheck-testing`. Este módulo é dependência transitiva via `api()`.

---

## Dependências do módulo

```
blindcheck-tracker
    └── api(blindcheck-testing)   ← modelos e interfaces públicas
```

---

## `TrackingEventStore`

Singleton que acumula eventos de acessibilidade capturados pelo serviço. O app de monitoramento e os drivers de teste leem daqui.

```kotlin
val store = TrackingEventStore.shared
```

### Gravação

```kotlin
store.startRecording()
store.stopRecording()
store.setRecording(true)
```

### Filtros

```kotlin
// Filtrar por pacote (só grava eventos do app alvo)
store.addTargetPackage("com.seu.app")
store.removeTargetPackage("com.seu.app")
store.clearTargetPackages()

// Filtrar por tipo de evento
store.addTargetEventType("TYPE_VIEW_ACCESSIBILITY_FOCUSED")
store.removeTargetEventType("TYPE_VIEW_ACCESSIBILITY_FOCUSED")
store.clearTargetEventTypes()
```

### Leitura

```kotlin
// Todos os eventos gravados (respeitando filtros de tipo se configurados)
val events: List<A11yEventRecord> = store.snapshot()
```

---

## `A11yEventNormalizer`

Converte `AccessibilityEvent` do Android em `A11yEventRecord` imutável.

```kotlin
val normalizer = A11yEventNormalizer()
val record = normalizer.normalize(event)
```

Normaliza: packageName, eventType, className, lista de textos, contentDescription. Delega a normalização do nó fonte para `A11yNodeNormalizer`.

---

## `A11yNodeNormalizer`

Converte `AccessibilityNodeInfo` em `A11yNodeSnapshot`, percorrendo a subárvore de forma segura.

```kotlin
val normalizer = A11yNodeNormalizer(
    maxDepth = 8,    // padrão
    maxNodes = 250,  // padrão
)
val snapshot = normalizer.normalize(node)
```

Protege contra loops e árvores muito grandes através de `maxDepth` e `maxNodes`. Recicla todos os nós acessados.

---

## `RemoteActions`

Constantes das ações de controle remoto disparadas via broadcast ADB.

```kotlin
object RemoteActions {
    const val ACTION_NEXT            = "com.theustech.blindcheck.ACTION_NEXT"
    const val ACTION_PREVIOUS        = "com.theustech.blindcheck.ACTION_PREVIOUS"
    const val ACTION_ACTIVATE        = "com.theustech.blindcheck.ACTION_ACTIVATE"
    const val ACTION_BACK            = "com.theustech.blindcheck.ACTION_BACK"
    const val ACTION_SCROLL_FORWARD  = "com.theustech.blindcheck.ACTION_SCROLL_FORWARD"
    const val ACTION_SCROLL_BACKWARD = "com.theustech.blindcheck.ACTION_SCROLL_BACKWARD"
    const val ACTION_HOME            = "com.theustech.blindcheck.ACTION_HOME"
    const val ACTION_RECENTS         = "com.theustech.blindcheck.ACTION_RECENTS"
    const val ACTION_SWIPE_UP        = "com.theustech.blindcheck.ACTION_SWIPE_UP"
    const val ACTION_SWIPE_DOWN      = "com.theustech.blindcheck.ACTION_SWIPE_DOWN"
}
```

---

## `AccessibilityEventType`

Enum com todos os tipos de eventos de acessibilidade suportados, mapeando os valores do Android framework para nomes legíveis.

```kotlin
AccessibilityEventType.ViewAccessibilityFocused  // TYPE_VIEW_ACCESSIBILITY_FOCUSED
AccessibilityEventType.WindowStateChanged         // TYPE_WINDOW_STATE_CHANGED
AccessibilityEventType.ViewTextChanged            // TYPE_VIEW_TEXT_CHANGED
// ... 13 tipos no total
```

Útil para configurar filtros no `TrackingEventStore` com type-safety.
