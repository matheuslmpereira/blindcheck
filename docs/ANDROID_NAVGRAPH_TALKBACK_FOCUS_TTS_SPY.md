# NavGraph, foco do TalkBack e rótulos repetidos: investigação com TTS spy

## Problema

Em uma sequência de destinos do Navigation Compose, três telas têm a mesma ação acessível: `Continuar`. Ao ativar a ação na Tela 1, alguns contextos de Android e TalkBack mantêm o foco de acessibilidade associado ao nó semanticamente equivalente na Tela 2. Para a pessoa usuária, isso pode soar como `Continuar` outra vez, sem deixar claro que a tela mudou.

Há uma hipótese adicional de testes manuais: ao tentar mover o foco imperativamente para o começo do destino novo, uma fala pendente da tela anterior pode aparecer antes da fala da nova tela.

Este documento registra o que foi observado em um emulador controlado. Não é uma alegação de comportamento idêntico em todas as versões de Android ou TalkBack.

## Por que eventos de acessibilidade não bastam

`TYPE_VIEW_ACCESSIBILITY_FOCUSED` mostra o nó que recebeu foco, mas não prova o texto enviado ao mecanismo de fala. Quando `com.theustech.blindcheck_tracking_app` é selecionado como engine TTS do sistema, o TTS spy registra cada `SynthesisRequest` como `TTS <texto>` em `BlindCheckAnnounce`.

| Evidência | O que demonstra |
| --- | --- |
| `WIN` | mudança anunciada na janela/painel de acessibilidade |
| `FOCUS` | nó que recebeu foco de acessibilidade |
| `TTS` | texto efetivamente solicitado ao engine TTS controlado |

O spy registra a solicitação enviada ao engine selecionado; não captura áudio, earcons, nem a fala se o TalkBack estiver usando outro engine.

## Ambiente e método

Execução em 17 de agosto de 2026, reexecutada em 18 de agosto de 2026 com as
duas abordagens novas:

* emulador `Medium_Phone`, API 36;
* Google TalkBack e `TrackingAccessibilityService` habilitados;
* engine TTS `com.theustech.blindcheck_tracking_app` selecionado;
* app de teste e tracking app instalados a partir deste repositório.

Antes da navegação, a captura foi validada com:

```text
TTS_SMOKE_REQUEST default BlindCheck_TTS_smoke_test
TTS BlindCheck_TTS_smoke_test
```

As ações foram enviadas pelo contrato de usuário do BlindCheck (`next` e `activate`). O log foi limpo imediatamente antes de cada ativação analisada:

```bash
adb logcat -c
make activate
sleep 3
adb logcat -d -s BlindCheckAnnounce:I -s BlindCheckTracker:D
```

Esse recorte evita atribuir à Tela 2 uma fala de `Continuar` que já tinha sido emitida na Tela 1.

## Resultado 1 — NavGraph original reproduz a ambiguidade

No cenário `Iniciar navegação por NavGraph`, o foco foi colocado em `Continuar` na Tela 1 e a ação foi ativada. A árvore Android coletada ao final continha `Tela 2`, mas a saída observada após a ativação foi:

```text
TTS Continuar
FOCUS Continuar, Botão
TTS Button
TTS Double-tap to activate
```

O TTS spy confirma que a nova interação solicita novamente `Continuar`. Como esse é o mesmo rótulo que a pessoa acabara de ouvir na Tela 1, a saída não comunica por si só que houve troca de destino. Isso explica a percepção de “Continuar lido duas vezes”: há uma fala antes da ativação na Tela 1 e outra, indistinguível, já no destino seguinte.

## Resultado 2 — removendo o Home para isolar origem e destino

O primeiro experimento imperativo usava `Ir para home` como primeiro item nas duas
telas. Isso tornava impossível determinar pelo texto se duas solicitações vinham
do Home antigo e do Home novo. Por isso, a matriz isolada passou a omitir o botão
Home e a usar o título — `Tela 1` ou `Tela 2` — como primeiro item acessível.

Essa mudança também revelou um confundidor na implementação inicial: depois de
`ACTION_ACCESSIBILITY_FOCUS`, o protótipo enviava manualmente
`TYPE_VIEW_SELECTED`. Com as duas operações, o spy registrou:

```text
TTS Tela 2
TTS Tela 2
FOCUS Tela 2
```

Depois de remover o evento sintético e manter exclusivamente
`ACTION_ACCESSIBILITY_FOCUS`, as três execuções registraram:

```text
TTS Tela 2
FOCUS Tela 2
```

Não houve `TTS Tela 1` após a ativação. Assim, neste ambiente, a hipótese de
fala residual da tela anterior **não foi reproduzida**. A duplicação anterior
era causada pelas duas operações sobre o mesmo nó da Tela 2.

## Resultado 3 — lifecycle da `Screen`, sem atraso fixo

O experimento imperativo passou a renderizar cada destino pela abstração
`Screen`. Ela só solicita foco quando o `NavBackStackEntry` está em `RESUMED`, o
container-raiz já teve layout e a tela registrou seu primeiro alvo nativo de
acessibilidade. Se esse alvo ainda não estiver anexado ou medido, a própria
`View` aguarda esses eventos. A ação é então publicada no próximo frame do
Compose, sem `postDelayed` e sem aguardar dados remotos da tela.

Essa espera é cancelável: ao sair da composição, o `LaunchedEffect` é cancelado
e remove os listeners nativos de attach e layout. Isso impede que uma `View` de
um destino já descartado retenha listeners ou receba um foco tardio.

As três execuções completas da matriz após essa mudança produziram, para foco
imperativo:

```text
TTS Tela 2
FOCUS Tela 2
```

O TTS spy continua sendo a verificação de integração mais fiel desse ponto. O
`UiAutomation` usado pelos testes instrumentados por vezes não expõe o estado
`accessibilityFocused` mantido pelo TalkBack, apesar de o serviço receber o
evento `FOCUS`; por isso os testes instrumentados validam a troca de destino e
a estrutura semântica, e a matriz controlada valida o feedback/foco real.

## Resultado 4 — matriz de abordagens isoladas

Cada abordagem foi executada três vezes. As 27 transições terminaram na Tela 2
e produziram a mesma sequência em todas as repetições:

| Abordagem | Foco final | Sequência TTS após ativar |
| --- | --- | --- |
| baseline | `Continuar` | `Continuar → Button → Double-tap to activate` |
| rótulos únicos (controle) | `Tela 2` | `Tela 2` |
| IDs únicos por tela | `Continuar` | `Continuar → Button → Double-tap to activate` |
| semântica recriada com `key(page)` | `Continuar` | `Continuar → Button → Double-tap to activate` |
| `paneTitle` | `Continuar` | `Tela 2 → Continuar → Button → Double-tap to activate` |
| foco imperativo somente com `ACTION_ACCESSIBILITY_FOCUS` | `Tela 2` | `Tela 2` |
| reset agnóstico pela lib (`:blindcheck-focus`) | `Tela 2` | `Tela 2` |
| aposentar a tela que sai | *não medido* | *não medido* |
| rótulos únicos + `paneTitle` (controle) | `Tela 2` | `Tela 2 → Tela 2` |
| reset combinado legado (como o app entrega hoje) | `Ir para home` | `Tela 2 → Ir para home → Button → Double-tap to activate` |

### IDs de foco

O Compose permite publicar `Modifier.testTag` como `viewIdResourceName` usando
`testTagsAsResourceId`. O teste instrumentado confirmou que a Tela 1 expõe
`navgraph_continue_page_1` e a Tela 2 expõe `navgraph_continue_page_2`.

Isso fornece IDs estáveis para UiAutomator e para as asserções do BlindCheck,
mas não cria um “ID de foco” reconhecido pelo TalkBack. No emulador testado, o
cenário com IDs únicos teve exatamente o mesmo foco e a mesma sequência TTS do
baseline. O ID é útil para automação; não é um mecanismo confiável para mudar a
heurística de foco do leitor de tela.

## Resultado 5 — reset agnóstico como biblioteca

O protótipo imperativo dependia de infraestrutura na tela: uma classe base, um
registro explícito do primeiro alvo e uma `AndroidView` nativa para que existisse
uma `View` na qual chamar `performAccessibilityAction`. Isso resolvia o caso do
experimento, mas não era aplicável a uma tela Compose comum e ainda introduzia
uma variável a mais em relação ao baseline: a árvore do cenário imperativo não
era a mesma do baseline.

`:blindcheck-focus` reimplementa o contorno sem nada disso. A tela aplica
`Modifier.resetAccessibilityFocusOnEnter(key = backStackEntry.id)` no seu
container-raiz e não registra coisa alguma. O alvo é resolvido a partir da árvore
semântica da própria subárvore do modifier — primeiro por `traversalIndex`,
depois de cima para baixo e da esquerda para a direita — e recebe
`ACTION_ACCESSIBILITY_FOCUS` num frame real do Compose, sem atraso fixo e sem
evento sintético.

O cenário `agnostic-focus-reset` mantém os rótulos ambíguos do baseline
(`Continuar` nas três telas) e é Compose puro. Nas três execuções:

```text
TTS Tela 2
FOCUS Tela 2
```

Ou seja: o mesmo resultado do foco imperativo, sem `AndroidView`, sem registro
por tela e sem conhecimento do conteúdo.

## Resultado 6 — a combinação que o app entrega não resolve o caso

Até esta rodada a matriz media apenas as variáveis isoladas. O cenário marcado
como solução no app — `legacy-combined-reset`, que combina semântica recriada,
`paneTitle` e foco imperativo, e mantém o botão Home no topo — nunca tinha sido
medido. Medido agora, nas três execuções:

```text
TTS Tela 2
TTS Ir para home
TTS Button
TTS Double-tap to activate
FOCUS Ir para home, Botão
```

O `paneTitle` comunica a troca de contexto, mas o foco é reiniciado no primeiro
item visual — o botão Home, cujo rótulo é idêntico nas três telas. O reset
acontece; a ambiguidade que originou a investigação, não. Isso reforça que o
alvo do reset precisa ser o primeiro item **do conteúdo do destino**, e é
exatamente o que a biblioteca isola: o chrome fora da subárvore do modifier
nunca é escolhido.

## Resultado 7 — aposentar a tela que sai (implementado, ainda não medido)

As duas abordagens que funcionam terminam com alguém **pedindo** foco. Este
cenário testa a premissa oposta: a de que o leitor mantém o foco em um nó
equivalente apenas **enquanto esse nó existe**.

No cenário `retire-leaving-screen`, ao trocar de destino:

1. o destino que sai limpa o foco de acessibilidade que estiver dentro da sua
   própria subárvore (`ACTION_CLEAR_ACCESSIBILITY_FOCUS`), enquanto os nós ainda
   existem — limpar depois de remover deixaria o leitor segurando um nó que não
   responde mais;
2. a subárvore inteira é publicada como um nó sem propriedade alguma, o que a
   torna inelegível para o leitor;
3. ninguém pede foco. Onde o leitor vai parar é a resposta da plataforma, e é
   exatamente isso que o cenário mede.

A borda usada é o `NavBackStackEntry` caindo abaixo de `RESUMED`: durante a
transição, os dois destinos estão compostos ao mesmo tempo, que é a janela em
que o leitor decide para onde o foco vai.

**Este cenário ainda não foi medido.** A matriz exige emulador dedicado com
TalkBack e o engine TTS do BlindCheck, e nenhum dispositivo estava conectado
quando ele foi implementado. A linha da matriz é obtida com:

```bash
NAVGRAPH_TTS_RUNS=3 make navgraph-tts-matrix
```

O runner já inclui `retire-leaving-screen`. O que os testes instrumentados
garantem hoje é o modo de falha perigoso desta estratégia: aposentar o lado
errado da transição produz uma tela que **renderiza normalmente e é invisível
para o leitor**, o que nenhuma verificação visual pega. Por isso
`NavGraphRetireLeavingScreenTest` e `HomeFocusResetComparisonTest` afirmam que o
destino que chega continua legível depois da troca.

Três resultados possíveis, e todos são informação:

| Se a saída for | Significa |
| --- | --- |
| `Tela 2` | a heurística do leitor depende do nó equivalente existir; o contorno pode ser feito sem pedir foco |
| `Continuar` | o leitor recoloca o foco por posição, não por identidade do nó; remover a tela antiga não basta |
| nada, ou foco perdido | limpar o foco sem oferecer destino deixa a pessoa sem contexto — pior que o baseline |

## O que a comunidade faz com esse bug

A busca por referências externas foi feita para responder à dúvida de coesão
arquitetural: até que ponto o contorno deste repositório é idiossincrático.

**O bug é conhecido e está aberto.** O registro público mais próximo do caso é
[b/272065229 — *Navigating between screens causes TalkBack to focus…*](https://issuetracker.google.com/issues/272065229).
A página exige login, então o conteúdo não pôde ser lido aqui; o título e a
existência são verificáveis pela busca. A vizinhança de issues sobre foco do
TalkBack no Compose é grande e antiga —
[276915102](https://issuetracker.google.com/issues/276915102),
[186443263](https://issuetracker.google.com/issues/186443263),
[252992211](https://issuetracker.google.com/issues/252992211) —, o que sustenta
a leitura de que não existe hoje uma resposta oficial para "reiniciar o foco no
novo destino".

**Não existe API pública para mover foco de acessibilidade.** A distinção
aparece em todas as fontes: `FocusRequester` move foco de *teclado*, não o do
leitor de tela. A recomendação da lista oficial de acessibilidade do Android é
que o único caminho é **enviar um evento de acessibilidade**
([eyes-free-dev](https://groups.google.com/g/eyes-free-dev/c/-ZtFWgwYahA)).
Eevis Panula documenta o contorno que circula na comunidade — alternar
`semantics { focused = true }` e desligar em seguida via `LaunchedEffect` —
descrevendo-o explicitamente como não-oficial, achado em discussão de
comunidade, e com boilerplate de estado
([eevis.codes](https://eevis.codes/blog/2025-07-07/its-all-about-accessibility-focus-and-compose/)).
Esse contorno é uma terceira forma de fazer o que `imperative-focus` e
`agnostic-focus-reset` já fazem, com a diferença de exigir estado na tela.

**A plataforma empurra para `paneTitle`, não para foco imperativo.** O sinal mais
forte encontrado não é um blog: é o Material Components para Android
[removendo](https://github.com/material-components/material-components-android/commit/516240d4e5519838305e4129235f964a5f82ef16)
o código que trocava foco no bottom sheet, em favor do título de painel — *"Also
removed the code to switch focus to the sheet on expansion in favor of this
approach to align with TalkBack's APIs."* Um `View` com `accessibilityPaneTitle`
emite `TYPE_WINDOW_STATE_CHANGED` ao aparecer, desaparecer ou mudar de título, e
essa é a API que o TalkBack entende como "esta região se comporta como janela"
([Android Developers](https://developer.android.com/guide/topics/ui/accessibility/principles)).
A documentação também alerta que o TalkBack usa o título de painel **como
identificador**, e que reaproveitar títulos genéricos entre telas independentes
causa comportamento imprevisível — o que é coerente com o Resultado 4, em que
`paneTitle` anunciou a troca de contexto mas não moveu o foco.

**Outras plataformas têm o mesmo problema e a mesma falta de resposta.** No
ecossistema React Native, a ordem de foco do último item ativo é preservada ao
navegar, e a correção é feita à mão por tela
([discussão em react-native-screens](https://github.com/software-mansion/react-native-screens/discussions/2874)).
Há também o caso inverso, que vale como aviso para o Resultado 7: limpar foco
antes da navegação deixou "o TalkBack focando primeiro a tela que desaparece"
([issue #3147](https://github.com/software-mansion/react-native-screens/issues/3147)).

### O que isso muda para a adoção

| Pergunta | Resposta pela evidência externa |
| --- | --- |
| O contorno é idiossincrático? | Não. É a mesma família de contorno que a comunidade usa, com menos acoplamento à tela |
| Existe API oficial melhor? | Não para mover foco. Para *anunciar* a troca, sim: `paneTitle` |
| A plataforma prefere qual mecanismo? | `paneTitle`. O Material Components migrou nessa direção |
| O que falta ser garantido? | Todas as fontes descrevem comportamento, nenhuma garante. O TTS spy continua sendo a única verificação |

A leitura defensável para outros projetos é **combinar, não escolher**:
`paneTitle` para comunicar a troca de contexto, porque é a API que a plataforma
reconhece, mais um reset de foco para o primeiro item do conteúdo, porque o
`paneTitle` sozinho deixa o foco no rótulo ambíguo. A matriz já tem os dois
isolados; o que ela ainda não tem é a combinação `paneTitle` + reset agnóstico
medida como um cenário próprio.

## Deliberação

Rótulos diferentes não são uma solução para este caso. Eles existem somente
como grupo de controle para demonstrar que a ambiguidade desaparece quando a
identidade textual muda.

IDs por página e `key(page)` não alteraram o comportamento do TalkBack. O
`paneTitle` anunciou a mudança de contexto, mas manteve o foco em `Continuar`.
As duas abordagens que colocaram o foco no primeiro item e produziram uma única
fala aplicam `ACTION_ACCESSIBILITY_FOCUS` ao nó novo, sem enviar um segundo
evento sintético: o protótipo imperativo e a biblioteca agnóstica. Entre as
duas, a biblioteca é a que pode ser adotada por uma tela Compose qualquer, e é
a única que garante que o alvo vem da subárvore do próprio destino.

Esse resultado torna o reset de foco um contorno viável para o projeto, mas
ainda não uma garantia geral: ele depende do timing entre Compose, a árvore
Android e a versão do leitor de tela. A adoção segue protegida por testes
instrumentados e TTS spy, e precisa ser validada em mais combinações de Android
e TalkBack antes de virar comportamento padrão.

## Regressão automatizada

Os testes instrumentados com TalkBack são opt-in e rodam com:

```bash
./gradlew :blindcheck-test-app:connectedDebugAndroidTest -PrunTalkBackFocusTests=true
```

O argumento é declarado pelo DSL do Android porque
`-Pandroid.testInstrumentationRunnerArguments.*` é descartado com configuration
cache, o que fazia esses testes serem silenciosamente pulados.

O runner versionado executa a matriz com:

```bash
NAVGRAPH_TTS_RUNS=3 make navgraph-tts-matrix
```

Ele:

1. selecionar o engine TTS do BlindCheck e executar o smoke test;
2. navegar até o botão da Tela 1 exclusivamente por `next`;
3. limpar o log imediatamente antes de `activate`;
4. aguardar a estabilização e coletar as linhas `WIN`, `FOCUS` e `TTS`;
5. grava os logs, a árvore Android e `summary.tsv` em
   `blindcheck-test-app/build/reports/navgraph-tts-spy/`.

O teste precisa preservar o estado anterior do engine TTS e dos serviços de acessibilidade. Ele deve rodar apenas em emulador dedicado, pois a captura guarda textos de interface que podem ser sensíveis.

## Limitações

* O TTS spy é fiel ao texto entregue ao engine controlado, não à mixagem de áudio nem a sons não verbais do TalkBack.
* TalkBack, Android, idioma, velocidade de fala e configurações do usuário podem alterar a ordem e a fragmentação das solicitações.
* Este resultado é evidência de regressão no ambiente descrito; a validação em dispositivos e versões adicionais continua necessária.
