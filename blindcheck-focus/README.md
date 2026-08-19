# blindcheck-focus

Biblioteca Android (Compose) que reinicia o foco de acessibilidade no primeiro item da nova tela.

**É uma dependência do app, não dos testes.** Diferente de `:blindcheck-testing`, que valida a
experiência, este módulo corrige um comportamento em tempo de execução.

---

## O problema

Ao trocar de destino no Navigation Compose, o Android tenta manter o foco de acessibilidade no nó
que considera equivalente ao anterior. Quando duas telas expõem a mesma ação — o clássico
`Continuar` em cada etapa de um fluxo — a pessoa ouve o mesmo rótulo de novo e nada comunica que a
tela mudou.

A captura controlada de TTS registrada em
[docs/ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md](../docs/ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md)
mostra o baseline pedindo `Continuar` outra vez logo após a ativação.

---

## Uso

```kotlin
implementation(project(":blindcheck-focus"))
```

```kotlin
composable("checkout") { backStackEntry ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .resetAccessibilityFocusOnEnter(key = backStackEntry.id),
    ) {
        Text("Pagamento")
        Button(onClick = ::next) { Text("Continuar") }
    }
}
```

Não há nada a registrar, herdar ou anotar na tela. `key` identifica o conteúdo exibido — em
Navigation Compose, o `NavBackStackEntry.id` — e o reset acontece **uma vez por valor**, para nunca
puxar o foco de volta enquanto a pessoa navega dentro do destino.

| Parâmetro | Efeito |
|---|---|
| `key` | Rearma o reset quando muda; um reset por valor |
| `enabled` | `false` mantém o comportamento padrão da plataforma |
| `strategy` | Qual das duas abordagens medidas aplicar (abaixo) |
| `isShowing` | `false` enquanto o destino está saindo |

---

## As duas estratégias

O módulo expõe **um** modifier com duas estratégias, e não dois modifiers: são
alternativas para medir uma contra a outra, não dois conceitos que a tela
precise aprender.

### `MoveFocusToFirstItem` (padrão)

O conteúdo que chega toma o foco: o primeiro item acessível da subárvore recebe
`ACTION_ACCESSIBILITY_FOCUS`. É a estratégia validada pela captura controlada de
TTS, e a única que já tem resultado medido.

### `RetireLeavingContent`

O conteúdo que sai é que se retira: o foco de acessibilidade dentro da subárvore
é liberado e a subárvore inteira é publicada como um nó sem propriedade alguma —
inelegível para o leitor. **Ninguém pede foco.** A premissa é que o leitor mantém
o foco em um nó equivalente apenas enquanto esse nó existe.

A ordem importa: o foco é liberado enquanto os nós que o seguram ainda existem.
Descartar a subárvore antes deixaria o leitor segurando um nó que não responde.

Como isso acontece na saída, o host precisa dizer quando o conteúdo deixa de ser
o atual — é para isso que serve `isShowing`. Em Navigation Compose, é o
`NavBackStackEntry` caindo abaixo de `RESUMED`, o que acontece enquanto os dois
destinos ainda estão compostos.

```kotlin
Modifier.resetAccessibilityFocusOnEnter(
    key = backStackEntry.id,
    strategy = AccessibilityFocusResetStrategy.RetireLeavingContent,
    isShowing = backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
)
```

**Esta estratégia ainda não foi medida com leitor de tela real.** O resultado
esperado, e os três desfechos possíveis, estão em
[docs/ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md](../docs/ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md#resultado-7--aposentar-a-tela-que-sai-implementado-ainda-não-medido).
Até lá, o padrão continua sendo `MoveFocusToFirstItem`.

---

## Como o alvo é escolhido

O módulo é agnóstico ao conteúdo: não conhece títulos, app bars nem botões.

1. O modifier marca sua própria subárvore com uma propriedade semântica interna.
2. Quando a subárvore é medida e há um leitor de tela ativo, o alvo é resolvido a partir da árvore
   semântica **apenas dessa subárvore** — nunca de chrome externo nem de um destino que ainda está
   saindo da composição.
3. Entre os nós elegíveis vence o primeiro na ordem de leitura: `traversalIndex`, depois de cima
   para baixo, depois da esquerda para a direita.
4. O foco é movido com `ACTION_ACCESSIBILITY_FOCUS`. Nenhum evento sintético é enviado — enviar um
   segundo evento sobre o mesmo nó duplicava a fala.

Um nó é elegível quando não está oculto da acessibilidade, ocupa espaço e descreve a si mesmo
(texto, `contentDescription`, `stateDescription`) ou expõe uma ação.

A espera não usa atraso fixo: cada tentativa acontece em um frame real do Compose, e o efeito é
cancelado junto com a composição.

---

## Superfície de API e risco de atualização

O módulo depende apenas do SDK do Android e do Compose. Não usa nenhuma API deprecada nem nenhuma
que exija `@OptIn` experimental.

| Origem | O que é usado |
|---|---|
| SDK Android | `Context`, `View`, `AccessibilityManager`, `AccessibilityNodeInfo` |
| Compose runtime | `Composable`, `LaunchedEffect`, `remember`, `rememberSaveable`, `withFrameNanos` |
| Compose ui | `Modifier`, `LocalView`, `onGloballyPositioned`, `semantics`, `SemanticsNode`, `SemanticsOwner`, `SemanticsProperties`, `SemanticsActions`, `SemanticsPropertyKey`, `clearAndSetSemantics`, `RootForTest` |
| JDK | `AtomicLong` |

Duas decisões existem especificamente para reduzir risco de atualização:

* a propriedade "oculto da acessibilidade" é lida **pelo nome** na configuração semântica, porque o
  Compose renomeou `InvisibleToUser` para `HideFromAccessibility` e deprecou a primeira. As duas
  grafias são reconhecidas, sem depender do símbolo;
* o modifier é uma função `@Composable`, não `Modifier.composed`, que está em depreciação suave.

O único ponto de acoplamento a observar num upgrade do Compose é `RootForTest`, usado para ler a
árvore semântica a partir da `View` hospedeira. Não há caminho público alternativo hoje. O cast é
seguro (`as?`): se uma versão futura deixar de expor o owner ali, **o reset simplesmente não roda e
a tela mantém o comportamento padrão da plataforma** — sem crash e sem mudança de comportamento
além da perda do reset. O teste instrumentado `exposesTheSemanticsOwnerOfTheHostView` existe para
que esse cenário quebre o build, e não a produção.

## Limites conhecidos

* O reset depende do momento em que o Compose publica a árvore semântica e da versão do leitor de
  tela; a validação de fidelidade é o TTS spy, não o `UiAutomation`.
* Se a subárvore não expuser nenhum item acessível dentro de 30 frames, o reset desiste em silêncio
  e o comportamento padrão da plataforma permanece.
* O reset só roda com acessibilidade ativa no dispositivo. Isso vale também para
  a aposentadoria da subárvore: sem leitor de tela ativo, nada é removido da
  árvore de acessibilidade.
* `RetireLeavingContent` remove a subárvore com `clearAndSetSemantics`, porque na
  versão do Compose usada aqui a propriedade "oculto da acessibilidade" existe
  apenas como `invisibleToUser`, experimental e desde então renomeada. O efeito
  observável para um leitor é o mesmo; a API é estável.
* O modo de falha perigoso de `RetireLeavingContent` é aposentar o lado errado da
  transição: a tela renderiza normalmente e fica invisível para o leitor. Os
  testes instrumentados afirmam que o destino que chega continua legível
  justamente por isso.

---

## Testes

```bash
./gradlew :blindcheck-focus:testDebugUnitTest
```

```bash
./gradlew :blindcheck-focus:connectedDebugAndroidTest
```

Os testes de unidade cobrem a escolha do alvo e os dois gates — reset e aposentadoria; os
instrumentados cobrem a resolução na árvore semântica real, inclusive a troca de `key` e o
isolamento da subárvore.

A aposentadoria depende de acessibilidade ativa, que os testes instrumentados deste módulo não
ligam. Ela é coberta onde há leitor de tela de verdade: `NavGraphRetireLeavingScreenTest` no app de
teste, opt-in por `-PrunTalkBackFocusTests=true`, e a matriz de TTS.
