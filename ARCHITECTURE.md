# BlindCheck — Arquitetura do Sistema

BlindCheck é uma plataforma de testes de acessibilidade para Android voltada a usuários cegos. Ela valida a camada de acessibilidade observável: eventos, foco, content descriptions, estados e ações — da forma como um usuário de TalkBack os experimenta.

---

## Módulos

```mermaid
graph TD
    subgraph Android["Android (device)"]
        TApp["blindcheck-test-app\nApp de exemplo\n(alvo dos testes)"]
        TrkApp["blindcheck-tracking-app\nApp de monitoramento\n(UI do event stream)"]

        subgraph Libs["Bibliotecas Android"]
            Interactor["blindcheck-interactor\nAccessibilityService\n+ BroadcastReceiver"]
            Tracker["blindcheck-tracker\nNormalização de eventos\n+ TrackingEventStore"]
            Testing["blindcheck-testing\nModelos, asserções\n+ UserAccessibilityActions"]
        end
    end

    Desktop["blindcheck-desktop\nRemote control UI\n(JVM/Compose Desktop)"]

    TrkApp --> Interactor
    TrkApp --> Tracker
    Interactor --> Tracker
    Tracker --> Testing
    TApp -.->|androidTestImplementation| Testing

    Desktop -.->|"ADB broadcast"| TrkApp
```

> Linhas sólidas = dependência de compilação. Linha tracejada do Desktop = chamada de processo (ADB).

---

## Responsabilidade de cada módulo

| Módulo | Tipo | Responsabilidade |
|---|---|---|
| `blindcheck-testing` | Android lib | Modelos de dados imutáveis, interface `UserAccessibilityActions`, asserções de foco, driver de teste |
| `blindcheck-tracker` | Android lib | Normalização de `AccessibilityEvent`/`NodeInfo`, `TrackingEventStore`, constantes de ação remota |
| `blindcheck-interactor` | Android lib | `TrackingAccessibilityService`, `RemoteActionReceiver`, injeção de gestos |
| `blindcheck-tracking-app` | Aplicativo | UI Compose para stream de eventos em tempo real, filtros, exportação |
| `blindcheck-test-app` | Aplicativo | App de exemplo determinístico (Login → Lista de Frutas → Detalhe) |
| `blindcheck-desktop` | JVM Desktop | Remote control com botões que disparam broadcasts via ADB |

---

## Modelos de dados centrais

```mermaid
classDiagram
    class A11yEventRecord {
        +String id
        +Long timestamp
        +String packageName
        +String eventType
        +String? className
        +List~String~ text
        +String? contentDescription
        +A11yNodeSnapshot? sourceNode
    }

    class A11yNodeSnapshot {
        +String? text
        +String? contentDescription
        +String? className
        +String? viewIdResourceName
        +String? packageName
        +Boolean clickable
        +Boolean enabled
        +Boolean focused
        +Boolean selected
        +Boolean checked
        +Boolean editable
        +List~String~ actions
        +RectSnapshot boundsInScreen
        +List~A11yNodeSnapshot~ children
    }

    class RectSnapshot {
        +Int left
        +Int top
        +Int right
        +Int bottom
    }

    class BlindCheckFlow {
        +String targetPackage
        +Long startedAt
        +Long? endedAt
        +String schemaVersion
        +List~A11yEventRecord~ events
    }

    A11yEventRecord --> A11yNodeSnapshot : sourceNode
    A11yNodeSnapshot --> RectSnapshot : boundsInScreen
    A11yNodeSnapshot --> A11yNodeSnapshot : children
    BlindCheckFlow --> A11yEventRecord : events
```

Todos os modelos são data classes Kotlin imutáveis e sem dependência do Android framework — serializáveis para JSON.

---

## Pipeline de captura de eventos

```mermaid
sequenceDiagram
    participant System as Android System
    participant Service as TrackingAccessibilityService
    participant EvNorm as A11yEventNormalizer
    participant NodeNorm as A11yNodeNormalizer
    participant Store as TrackingEventStore

    System->>Service: onAccessibilityEvent(event)
    Service->>EvNorm: normalize(event)
    EvNorm->>NodeNorm: normalizeNode(sourceNode, depth=0, budget)
    loop até maxDepth=8 ou maxNodes=250
        NodeNorm->>NodeNorm: normalizeNode(child, depth+1, budget)
    end
    NodeNorm-->>EvNorm: A11yNodeSnapshot
    EvNorm-->>Service: A11yEventRecord
    Service->>Store: record(event)
    Note over Store: filtra por pacote e tipo\nse recording=true
```

---

## Fluxo de controle remoto (Desktop → Gesto)

```mermaid
sequenceDiagram
    participant User as Usuário
    participant Desktop as blindcheck-desktop
    participant ADB as ADB
    participant Receiver as RemoteActionReceiver
    participant Service as TrackingAccessibilityService
    participant Android as Android Framework

    User->>Desktop: clica "Next"
    Desktop->>ADB: am broadcast -a ACTION_NEXT
    ADB->>Receiver: onReceive(intent)
    Receiver->>Service: executor.execute("ACTION_NEXT")
    Service->>Android: dispatchGesture(swipeRight 150ms)
    Android-->>Service: AccessibilityEvent (foco mudou)
    Service->>Service: onAccessibilityEvent → record()
```

O `RemoteActionReceiver` valida se a ação está no conjunto `SUPPORTED_ACTIONS` antes de delegar. O `executor` é a própria instância do service — `TrackingAccessibilityService` implementa `ActionExecutor`.

---

## Gestos implementados

```mermaid
flowchart LR
    subgraph Ações["execute(action)"]
        NEXT["ACTION_NEXT\n→ swipe direita 25%→75% w\n150ms"]
        PREV["ACTION_PREVIOUS\n→ swipe esquerda 75%→25% w\n150ms"]
        ACT["ACTION_ACTIVATE\n→ dois toques no centro\n0ms + 100ms offset"]
        BACK["ACTION_BACK\n→ GLOBAL_ACTION_BACK"]
        SF["ACTION_SCROLL_FORWARD\n→ 2 dedos p/ cima 70%→30% h\n400ms"]
        SB["ACTION_SCROLL_BACKWARD\n→ 2 dedos p/ baixo 30%→70% h\n400ms"]
        HOME["ACTION_HOME\n→ GLOBAL_ACTION_HOME"]
        REC["ACTION_RECENTS\n→ GLOBAL_ACTION_RECENTS"]
        SU["ACTION_SWIPE_UP\n→ 1 dedo p/ cima 80%→20% h\n600ms"]
        SD["ACTION_SWIPE_DOWN\n→ 1 dedo p/ baixo 20%→80% h\n600ms"]
    end
```

Os gestos de navegação (next/previous) reproduzem exatamente o comportamento do TalkBack. O double-tap do `activate` usa dois `StrokeDescription` de 1ms com 100ms de intervalo, que o TalkBack interpreta como ativação do elemento focado.

---

## Fluxo de teste instrumentado

```mermaid
sequenceDiagram
    participant Test as Teste (instrumented)
    participant Driver as AndroidAccessibilityTestDriver
    participant Actions as AndroidUserAccessibilityActions
    participant UiDevice as UiDevice (UIAutomator)
    participant Store as TrackingEventStore
    participant Assertions as FocusSequenceExpectation

    Test->>Driver: actions().next()
    Driver->>Actions: next()
    Actions->>UiDevice: swipe(25%w, cy, 75%w, cy, steps=10)

    Test->>Driver: assertCurrentWindowContains(expectation)
    Driver->>Store: snapshot()
    Store-->>Driver: List~A11yEventRecord~
    Driver->>Assertions: assertMatches(events, package)
    Note over Assertions: compara texto, contentDescription\ne estados nó a nó
    Assertions-->>Test: ✓ ou AssertionError detalhado
```

O driver usa polling com retry (timeout 2s, intervalo 50ms) para aguardar o evento aparecer no store antes de falhar.

---

## TrackingEventStore — máquina de estado

```mermaid
stateDiagram-v2
    [*] --> Parado : init

    Parado --> Gravando : startRecording()
    Gravando --> Parado : stopRecording()

    state Gravando {
        [*] --> Filtrando
        Filtrando --> Armazenando : pacote ∈ targetPackages\n(ou sem filtro)
        Armazenando --> Filtrando : próximo evento
    }

    Gravando --> Gravando : addTargetPackage()\nremoveTargetPackage()\naddTargetEventType()
```

O store é um singleton (`TrackingEventStore.shared`). A lista interna é um `mutableListOf` protegido por `@Volatile` no flag `isRecording`. Os filtros de pacote e tipo são `LinkedHashSet` para preservar ordem de inserção.

---

## Diagrama de componentes (C4 nível 2)

```mermaid
C4Component
    title BlindCheck — Componentes

    Person(tester, "QA / Dev", "Escreve testes\nautomatizados")
    Person(operator, "Operador", "Usa o Desktop\npara controle manual")

    System_Boundary(desktop, "blindcheck-desktop") {
        Component(ui_desktop, "RemoteControlApp", "Compose Desktop", "Botões que disparam broadcasts via ADB")
    }

    System_Boundary(device, "Dispositivo Android") {
        System_Boundary(tracking, "blindcheck-tracking-app") {
            Component(ui_stream, "TrackingEventStreamScreen", "Compose", "Stream de eventos em tempo real")
        }
        System_Boundary(interactor_mod, "blindcheck-interactor") {
            Component(service, "TrackingAccessibilityService", "AccessibilityService", "Captura eventos e injeta gestos")
            Component(receiver, "RemoteActionReceiver", "BroadcastReceiver", "Recebe broadcasts de ação remota")
        }
        System_Boundary(tracker_mod, "blindcheck-tracker") {
            Component(store, "TrackingEventStore", "Singleton", "Buffer de eventos gravados")
            Component(normalizer, "A11yEventNormalizer", "Kotlin class", "Converte AccessibilityEvent → A11yEventRecord")
        }
        System_Boundary(testing_mod, "blindcheck-testing") {
            Component(driver, "AndroidAccessibilityTestDriver", "Test API", "Asserções sobre o estado atual")
            Component(actions, "AndroidUserAccessibilityActions", "UserAccessibilityActions", "Gestos via UiDevice")
            Component(models, "Models", "Data classes", "A11yEventRecord, A11yNodeSnapshot, RectSnapshot")
        }
        System_Boundary(testapp, "blindcheck-test-app") {
            Component(sample, "BlindCheckMockupApp", "Compose", "App de exemplo (Login + Frutas)")
        }
    }

    Rel(operator, ui_desktop, "Clica botões")
    Rel(tester, driver, "Usa em testes instrumentados")
    Rel(ui_desktop, receiver, "ADB broadcast", "am broadcast -a ACTION_*")
    Rel(receiver, service, "executor.execute()")
    Rel(service, normalizer, "normalize(event)")
    Rel(normalizer, store, "record(A11yEventRecord)")
    Rel(ui_stream, store, "snapshot() polling 500ms")
    Rel(driver, store, "snapshot() com retry")
    Rel(driver, actions, "delega navegação")
    Rel(actions, sample, "UiDevice gestures")
    Rel(service, sample, "dispatchGesture()")
```

---

## Makefile — comandos de operação

| Target | Descrição |
|---|---|
| `make enable-tracking` | Ativa o serviço de acessibilidade BlindCheck no dispositivo |
| `make enable-talkback` | Ativa o TalkBack junto ao BlindCheck |
| `make disable-talkback` | Desativa o TalkBack mantendo o BlindCheck |
| `make next` | Envia `ACTION_NEXT` via ADB |
| `make previous` | Envia `ACTION_PREVIOUS` via ADB |
| `make activate` | Envia `ACTION_ACTIVATE` via ADB |
| `make back` | Envia `ACTION_BACK` via ADB |
| `make scroll-forward` / `make scroll-backward` | Scroll com dois dedos |
| `make home` / `make recents` | Ações globais do sistema |
| `make swipe-up` / `make swipe-down` | Gestos verticais de dedo único |
| `make logcat` | Filtra logs das tags BlindCheck |

---

## Decisões de design relevantes

### 1. Gestos reais em vez de ações virtuais
`AccessibilityNodeInfo.performAction(ACTION_CLICK)` não funciona para UI do sistema (app drawer, launcher). Todos os comandos de navegação usam `AccessibilityService.dispatchGesture()` com `GestureDescription`, reproduzindo o comportamento físico que o TalkBack intercepta.

### 2. Separação de camadas
- **Modelos** não dependem do Android framework — são data classes puras, serializáveis.
- **blindcheck-testing** exporta a API pública de asserções sem expor `AccessibilityNodeInfo` ou `AccessibilityEvent`.
- **blindcheck-tracker** expõe `blindcheck-testing` via `api()`, tornando os modelos transitivos para os consumidores.

### 3. Manifesto por merge
O `blindcheck-interactor` declara o `<service>` e o `<receiver>` no próprio `AndroidManifest.xml`. Ao ser incluído como dependência, o Android Gradle Plugin faz o merge automático no manifesto do app — o `blindcheck-tracking-app` não precisa repetir as declarações.

### 4. `internal` como barreira de módulo
`TrackingAccessibilityService.executor` é `internal`, acessível apenas dentro de `blindcheck-interactor`. O `setExecutorForTest()` é `public` para permitir injeção em testes instrumentados de outros módulos, sem expor o campo diretamente.
