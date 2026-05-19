# blindcheck-testing

Biblioteca de testes de acessibilidade para Android. Fornece o driver de teste, asserções e modelos de dados para validar a experiência de usuários cegos em qualquer app Android.

Esta é a dependência que você adiciona ao seu projeto. Os demais módulos (`blindcheck-tracker`, `blindcheck-interactor`) são infraestrutura interna.

---

## Dependência

```kotlin
// app/build.gradle.kts
dependencies {
    androidTestImplementation(project(":blindcheck-testing"))
}
```

---

## Pacotes

| Pacote | O que contém |
|---|---|
| `android` | Driver de teste, ações, setup de acessibilidade |
| `assertions` | `FocusExpectation`, `FocusSequenceExpectation`, `FeedbackExpectation` |
| `model` | Modelos de dados imutáveis |
| `actions` | Interface `UserAccessibilityActions` |

---

## `AndroidAccessibilitySetup`

Configura o pipeline de acessibilidade do Android antes dos testes.

```kotlin
private val setup = AndroidAccessibilitySetup.create()

@Before
fun setUp() {
    setup.ensureAccessibilityEnabled() // obrigatório
    composeRule.waitForIdle()
}
```

| Método | Descrição |
|---|---|
| `ensureAccessibilityEnabled()` | Habilita acessibilidade nas configurações seguras. Seguro em emuladores. |
| `enableTalkBack()` | Ativa o TalkBack (requer TalkBack instalado no dispositivo). |
| `disableTalkBack()` | Desativa o TalkBack. Chamar no `@After` para não deixar o device sujo. |

---

## `AndroidAccessibilityTestDriver`

Ponto central de asserções. Lê a árvore de acessibilidade da janela ativa.

```kotlin
private val driver = AndroidAccessibilityTestDriver.create()
```

### Asserções

#### `assertCurrentWindowContains(expectation)`
Verifica que algum nó na janela satisfaz a expectativa. Retry automático (2s timeout, 50ms intervalo).

```kotlin
driver.assertCurrentWindowContains(FocusExpectation(textContains = "Bem-vindo"))
```

#### `assertFocused(expectation)`
Verifica que o nó com foco de acessibilidade atual satisfaz a expectativa.

```kotlin
driver.assertFocused(FocusExpectation(textContains = "E-mail", editable = true))
```

#### `assertCurrentWindowFeedback(expectation)`
Verifica presença de texto anunciável (text + contentDescription) na janela.

```kotlin
driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Campo obrigatório"))
```

#### `focusFirst(expectation): Boolean`
Move o foco de acessibilidade para o primeiro nó que satisfaz a expectativa. Retorna `false` se não encontrado.

```kotlin
val reached = driver.focusFirst(FocusExpectation(textContains = "Confirmar", clickable = true))
assertTrue(reached)
```

#### `currentWindowEvents(): List<A11yEventRecord>`
Retorna todos os nós da janela como eventos sintéticos. Entrada para `FocusSequenceExpectation`.

```kotlin
val events = driver.currentWindowEvents()
```

#### `currentWindowSnapshot(): A11yNodeSnapshot`
Snapshot completo da janela ativa como árvore de nós.

---

## `AndroidUserAccessibilityActions`

Implementa `UserAccessibilityActions` via `UiDevice`. Obtido via `driver.actions()`.

```kotlin
val actions = driver.actions()
```

Todos os métodos são `suspend`. Use `runTest {}` ou `runBlocking {}`.

| Método | TalkBack equivalente | Detalhe |
|---|---|---|
| `next()` | Swipe direita | Move foco para próximo elemento |
| `previous()` | Swipe esquerda | Move foco para elemento anterior |
| `activate()` | Duplo-toque | Ativa o elemento com foco |
| `scrollForward()` | 2 dedos p/ cima | Rola conteúdo para frente |
| `scrollBackward()` | 2 dedos p/ baixo | Rola conteúdo para trás |
| `inputText(value)` | Teclado | `ACTION_SET_TEXT` no nó editável com foco |
| `back()` | Botão voltar | `UiDevice.pressBack()` |

---

## `FocusExpectation`

Matcher de nó de acessibilidade. Todos os parâmetros são opcionais e combinados com AND.

```kotlin
data class FocusExpectation(
    val textEquals: String? = null,
    val textContains: String? = null,
    val contentDescriptionEquals: String? = null,
    val contentDescriptionContains: String? = null,
    val clickable: Boolean? = null,
    val editable: Boolean? = null,
    val enabled: Boolean? = null,
    val selected: Boolean? = null,
    val checked: Boolean? = null,
)
```

**Exemplos:**

```kotlin
// Campo de texto rotulado
FocusExpectation(textContains = "E-mail", editable = true)

// Botão habilitado
FocusExpectation(textContains = "Enviar", clickable = true)

// Botão desabilitado
FocusExpectation(textContains = "Enviar", clickable = true, enabled = false)

// Imagem com apenas contentDescription (sem texto visível)
FocusExpectation(contentDescriptionContains = "Foto de perfil")

// Checkbox marcado
FocusExpectation(textContains = "Aceito os termos", checked = true)
```

---

## `FocusSequenceExpectation`

Valida a **ordem de leitura** dos elementos na tela — o que o TalkBack anuncia à medida que o usuário faz swipes.

```kotlin
FocusSequenceExpectation(
    items = listOf(
        FocusExpectation(textContains = "Título da tela"),
        FocusExpectation(textContains = "Campo nome", editable = true),
        FocusExpectation(textContains = "Campo e-mail", editable = true),
        FocusExpectation(textContains = "Confirmar", clickable = true),
    )
).assertMatches(
    events = driver.currentWindowEvents(),
    targetPackage = "com.seu.app",
)
```

A verificação é de **sub-sequência**: elementos adicionais entre os esperados não causam falha. Garante a ordem relativa, não a exclusividade.

---

## `FeedbackExpectation`

Verifica que a janela expõe texto anunciável contendo a string. Útil para erros de validação e mensagens de estado.

```kotlin
FeedbackExpectation(contains = "Informe o e-mail")
```

---

## Modelos de dados

Todos são data classes Kotlin imutáveis, sem dependência do Android framework.

### `A11yNodeSnapshot`

Snapshot de um nó de acessibilidade com toda a sua subárvore.

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
    val checked: Boolean,
    val editable: Boolean,
    val actions: List<String>,
    val boundsInScreen: RectSnapshot,
    val children: List<A11yNodeSnapshot>,
)
```

### `A11yEventRecord`

Registro de um evento de acessibilidade capturado.

```kotlin
data class A11yEventRecord(
    val id: String,
    val timestamp: Long,
    val packageName: String?,
    val eventType: String,
    val className: String?,
    val text: List<String>,
    val contentDescription: String?,
    val sourceNode: A11yNodeSnapshot?,
)
```

### `RectSnapshot`

Bounds de um nó em coordenadas de tela.

```kotlin
data class RectSnapshot(val left: Int, val top: Int, val right: Int, val bottom: Int)
```

### `BlindCheckFlow`

Gravação completa de um fluxo de acessibilidade (usado com o app de tracking).

```kotlin
data class BlindCheckFlow(
    val targetPackage: String,
    val startedAt: Long,
    val endedAt: Long?,
    val schemaVersion: String,  // "blindcheck-flow-v1"
    val events: List<A11yEventRecord>,
)
```
