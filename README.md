# BlindCheck

Toolkit de acessibilidade Android para validar a experiência de usuários cegos. Permite explorar, controlar e testar qualquer app Android do ponto de vista do TalkBack — sem precisar ser usuário de leitor de tela.

---

## O que o BlindCheck faz

| Necessidade | Ferramenta |
|---|---|
| Explorar o fluxo TalkBack de um app manualmente | Desktop remote + tracking app |
| Escrever testes automatizados de navegação | `blindcheck-testing` |
| Validar a ordem de leitura dos elementos | `FocusSequenceExpectation` |
| Verificar que textos de erro são acessíveis | `assertCurrentWindowContains` |
| Simular gestos via terminal/CI | `makefile` / ADB broadcast |

---

## Início rápido

### Pré-requisitos

- Android Studio instalado
- ADB no PATH (ou `ANDROID_HOME` configurado)
- Dispositivo físico ou emulador conectado com TalkBack disponível
- Java 17+

### 1. Clone e build

```bash
git clone <repo>
cd blindcheck
```

### 2. Sessão de exploração manual (recomendado para começar)

Um único comando instala os apps, ativa o serviço e abre o desktop remote:

```bash
make session
```

Isso executa, em ordem:
1. Instala `blindcheck-tracking-app` e `blindcheck-test-app` no dispositivo
2. Ativa o serviço de acessibilidade BlindCheck (junto ao TalkBack)
3. Abre o `blindcheck-test-app` no dispositivo
4. Inicia o `blindcheck-desktop` em background

No desktop você verá o painel de controle e o log de anúncios em tempo real.

### 3. Rodar os testes automatizados

```bash
./gradlew connectedDebugAndroidTest
# ou
make test
```

Os testes ficam em `blindcheck-test-app/src/androidTest/`.

---

## Modo 1 — Exploração manual com o desktop

Use o `blindcheck-desktop` para explorar o fluxo TalkBack de qualquer app sem precisar tocar no dispositivo.

```
┌─────────────────────────────────┐
│        blindcheck-desktop       │
│                                 │
│  ● emulator-5554                │
│  ─────────────────────          │
│  Navegação                      │
│  [  ↑  ]                        │
│  [←] [OK] [→]     ← teclado!   │
│  [  ↓  ]                        │
│                                 │
│  Anúncios ──────────── [🗑] [Logcat]
│  ♿ E-mail, Campo de texto, editável
│  🔊 Informe o e-mail            │
│  📱 Acessar conta               │
└─────────────────────────────────┘
```

### Atalhos de teclado

| Tecla | Ação TalkBack |
|---|---|
| `→` | Próximo elemento (swipe direita) |
| `←` | Elemento anterior (swipe esquerda) |
| `Enter` / `Espaço` | Ativar elemento focado (duplo-toque) |
| `Esc` / `Backspace` | Voltar |
| `↑` | Scroll para frente (2 dedos p/ cima) |
| `↓` | Scroll para trás (2 dedos p/ baixo) |
| `Delete` | Limpar log |

### Log de anúncios

O painel de anúncios mostra em tempo real o que o TalkBack anuncia:

| Indicador | Tipo | Quando aparece |
|---|---|---|
| ♿ cinza | **FOCUS** | Elemento recebeu foco de acessibilidade |
| 🔊 laranja | **ANN** | Anúncio dinâmico, erro, live region |
| 📱 laranja | **WIN** | Mudança de tela ou diálogo |

Botão **Logcat** alterna para as linhas brutas do logcat — útil para debug.

### Controle via terminal (alternativa)

```bash
make next        # swipe direita
make previous    # swipe esquerda
make activate    # duplo-toque
make back        # voltar
make scroll-forward
make scroll-backward
make home
make recents
```

---

## Modo 2 — Testes automatizados

Adicione a dependência ao módulo que contém seus testes instrumentados:

```kotlin
// app/build.gradle.kts
dependencies {
    androidTestImplementation(project(":blindcheck-testing"))
}
```

### Setup mínimo

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<MainActivity>()

private val setup  = AndroidAccessibilitySetup.create()
private val driver = AndroidAccessibilityTestDriver.create()

@Before
fun setUp() {
    setup.ensureAccessibilityEnabled()
    composeRule.waitForIdle()
}
```

### Estilo A — Verificação estática

Verifica que um elemento existe e é acessível, sem navegar até ele:

```kotlin
@Test
fun loginScreen_emailField_isAccessible() {
    driver.assertCurrentWindowContains(
        FocusExpectation(textContains = "E-mail", editable = true)
    )
}

@Test
fun loginScreen_validationErrors_areAccessible() = runTest {
    val actions = driver.actions()
    // navega até o botão e submete vazio
    repeat(3) { actions.next() }
    actions.activate()
    composeRule.waitForIdle()

    driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe o e-mail"))
    driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe a senha"))
}
```

### Estilo B — Jornada de navegação

Simula swipe a swipe o que um usuário cego faria:

```kotlin
@Test
fun loginJourney_happyPath() = runTest {
    val actions = driver.actions()

    actions.next()   // → heading
    driver.assertFocused(FocusExpectation(textContains = "Acessar conta"))

    actions.next()   // → campo e-mail
    driver.assertFocused(FocusExpectation(textContains = "E-mail", editable = true))
    actions.activate()
    actions.inputText("dev@example.com")

    actions.next()   // → campo senha
    actions.activate()
    actions.inputText("123456")

    actions.next()   // → botão Entrar
    driver.assertFocused(FocusExpectation(textContains = "Entrar", clickable = true))
    actions.activate()

    composeRule.waitForIdle()
    driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
}
```

### Estilo C — Ordem de leitura

Valida que o TalkBack lê os elementos na sequência correta:

```kotlin
@Test
fun loginScreen_readingOrder_isCorrect() {
    FocusSequenceExpectation(
        items = listOf(
            FocusExpectation(textContains = "Acessar conta"),
            FocusExpectation(textContains = "E-mail", editable = true),
            FocusExpectation(textContains = "Senha",  editable = true),
            FocusExpectation(textContains = "Entrar", clickable = true),
        )
    ).assertMatches(
        events = driver.currentWindowEvents(),
        targetPackage = "com.seu.app",
    )
}
```

### Referência rápida — `FocusExpectation`

```kotlin
FocusExpectation(
    textContains = "parcial",               // texto do nó (parcial)
    textEquals   = "Exato",                 // texto do nó (exato)
    contentDescriptionContains = "Imagem",  // contentDescription
    clickable = true,    // deve ser clicável
    editable  = true,    // deve ser editável (campo de texto)
    enabled   = false,   // deve estar desabilitado
    checked   = true,    // checkbox/switch marcado
)
```

---

## Módulos

```
blindcheck/
├── blindcheck-testing/        ← sua dependência de teste
├── blindcheck-tracker/        ← infraestrutura interna (normalização de eventos)
├── blindcheck-interactor/     ← serviço de acessibilidade + broadcast receiver
├── blindcheck-tracking-app/   ← app companion instalado no dispositivo
├── blindcheck-desktop/        ← app desktop de controle remoto
└── blindcheck-test-app/       ← app de exemplo para validar a lib
```

| Módulo | Para quem | O que faz |
|---|---|---|
| [`blindcheck-testing`](blindcheck-testing/README.md) | Quem escreve testes | Driver, asserções, matchers |
| [`blindcheck-desktop`](blindcheck-desktop/README.md) | Quem explora manualmente | Remote control + log de anúncios |
| [`blindcheck-tracking-app`](blindcheck-tracking-app/README.md) | Instalado no dispositivo | Captura eventos, expõe broadcasts |
| [`blindcheck-interactor`](blindcheck-interactor/README.md) | Infraestrutura interna | `AccessibilityService` + gestos físicos |
| [`blindcheck-tracker`](blindcheck-tracker/README.md) | Infraestrutura interna | Normalização e store de eventos |
| [`blindcheck-test-app`](blindcheck-test-app/README.md) | Referência de uso | App de exemplo com login, lista e detalhe |

---

## Arquitetura em uma linha

```
Desktop remote ──(ADB broadcast)──▶ AccessibilityService ──▶ Gesto físico ──▶ TalkBack
                                          │
                              logcat "BlindCheckAnnounce"
                                          │
                          ◀──(polling 500ms)── Desktop log panel
```

Detalhes completos em [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Comandos úteis

```bash
make session          # instalação completa + desktop (início de sessão)
make test             # roda todos os testes instrumentados
make enable-tracker   # ativa o serviço de acessibilidade
make disable-a11y     # desativa acessibilidade no dispositivo
make check-a11y       # verifica serviços ativos
make logs             # logcat filtrado por tags BlindCheck
```
