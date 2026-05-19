# blindcheck-interactor

Biblioteca Android interna que contém o `AccessibilityService` e o `BroadcastReceiver` do sistema BlindCheck. É incluída como dependência do `blindcheck-tracking-app` e suas declarações de manifesto são mescladas automaticamente via Android Manifest Merger.

**Não é uma dependência de quem escreve testes.** É infraestrutura do sistema de rastreamento.

---

## Dependências do módulo

```
blindcheck-interactor
    └── implementation(blindcheck-tracker)
```

---

## `TrackingAccessibilityService`

`AccessibilityService` que captura eventos de acessibilidade de qualquer app ativo no sistema e os grava no `TrackingEventStore`. Também implementa `ActionExecutor`, executando os gestos correspondentes quando acionado pelo `RemoteActionReceiver`.

O serviço é declarado no manifesto da lib e registrado automaticamente ao instalar o `blindcheck-tracking-app`.

### Log de anúncios (`BlindCheckAnnounce`)

Além de gravar no store, o serviço emite logs de acessibilidade legíveis via logcat, consumidos pelo `blindcheck-desktop` em tempo real.

| Prefixo | Evento de origem | Exemplo |
|---|---|---|
| `FOCUS` | `TYPE_VIEW_ACCESSIBILITY_FOCUSED` | `FOCUS E-mail, Campo de texto, editável` |
| `ANN` | `TYPE_ANNOUNCEMENT` / `TYPE_WINDOW_CONTENT_CHANGED` | `ANN Informe o e-mail` |
| `WIN` | `TYPE_WINDOW_STATE_CHANGED` | `WIN Acessar conta` |

**Deduplicação:** textos já anunciados em um evento FOCUS imediatamente anterior são suprimidos de eventos ANN subsequentes para evitar ruído (ex.: label "E-mail" não reaparece como ANN quando o erro do mesmo campo dispara).

**Detecção de erro:** `TYPE_WINDOW_CONTENT_CHANGED` é processado quando:
- `node.error != null` → campo com erro ativo; extrai texto dos filhos
- `node.liveRegion != 0` → região de atualização automática
- `node.parent.error != null` → container de texto de suporte de um campo com erro

**Estrutura de traversal:** para elementos Compose (label, role, erro), o serviço percorre filhos e netos do nó focado, já que a árvore de acessibilidade do Compose distribui textos em nós filhos.

### Gestos implementados

| Ação | Gesto físico | Parâmetros |
|---|---|---|
| `ACTION_NEXT` | Swipe horizontal direita | 25%→75% da largura, 150ms |
| `ACTION_PREVIOUS` | Swipe horizontal esquerda | 75%→25% da largura, 150ms |
| `ACTION_ACTIVATE` | Duplo-toque no centro | 2 strokes de 1ms, 100ms de intervalo |
| `ACTION_SCROLL_FORWARD` | 2 dedos para cima | 70%→30% da altura, 400ms |
| `ACTION_SCROLL_BACKWARD` | 2 dedos para baixo | 30%→70% da altura, 400ms |
| `ACTION_SWIPE_UP` | 1 dedo para cima | 80%→20% da altura, 600ms |
| `ACTION_SWIPE_DOWN` | 1 dedo para baixo | 20%→80% da altura, 600ms |
| `ACTION_BACK` | — | `GLOBAL_ACTION_BACK` |
| `ACTION_HOME` | — | `GLOBAL_ACTION_HOME` |
| `ACTION_RECENTS` | — | `GLOBAL_ACTION_RECENTS` |

Todos os gestos usam `dispatchGesture()` com `GestureDescription` — gestos físicos reais que o TalkBack intercepta, não ações virtuais de acessibilidade.

### Para testes

```kotlin
// Injeta um executor falso para testes unitários/integração
TrackingAccessibilityService.setExecutorForTest(fakeExecutor)

// Limpa após o teste
TrackingAccessibilityService.setExecutorForTest(null)
```

---

## `RemoteActionReceiver`

`BroadcastReceiver` que escuta os 10 broadcasts de ação remota e delega ao executor do serviço.

Declarado no manifesto da lib com `android:exported="true"` e filtros para todas as actions de `RemoteActions`.

```kotlin
// Quem o chama (via ADB ou blindcheck-desktop):
adb shell am broadcast -p com.theustech.blindcheck_tracking_app \
    -a com.theustech.blindcheck.ACTION_NEXT
```

---

## `ActionExecutor`

Interface funcional implementada pelo `TrackingAccessibilityService`.

```kotlin
fun interface ActionExecutor {
    fun execute(action: String)
}
```

---

## Manifesto

O manifesto da lib declara o serviço e o receiver. Ao adicionar `:blindcheck-interactor` como dependência, o Android Gradle Plugin mescla essas declarações automaticamente no manifesto do app consumidor — sem necessidade de declarar manualmente.

```xml
<!-- declarado em blindcheck-interactor/src/main/AndroidManifest.xml -->
<service android:name="com.theustech.blindcheck_interactor.TrackingAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" ... />

<receiver android:name="com.theustech.blindcheck_interactor.RemoteActionReceiver"
    android:exported="true" ... />
```
