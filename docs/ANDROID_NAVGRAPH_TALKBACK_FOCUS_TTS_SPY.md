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

Execução em 17 de agosto de 2026:

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

Cada abordagem foi executada três vezes. As 21 transições terminaram na Tela 2
e produziram a mesma sequência em todas as repetições:

| Abordagem | Foco final | Sequência TTS após ativar |
| --- | --- | --- |
| baseline | `Continuar` | `Continuar → Button → Double-tap to activate` |
| rótulos únicos (controle) | `Tela 2` | `Tela 2` |
| IDs únicos por tela | `Continuar` | `Continuar → Button → Double-tap to activate` |
| semântica recriada com `key(page)` | `Continuar` | `Continuar → Button → Double-tap to activate` |
| `paneTitle` | `Continuar` | `Tela 2 → Continuar → Button → Double-tap to activate` |
| foco imperativo somente com `ACTION_ACCESSIBILITY_FOCUS` | `Tela 2` | `Tela 2` |
| rótulos únicos + `paneTitle` (controle) | `Tela 2` | `Tela 2 → Tela 2` |

### IDs de foco

O Compose permite publicar `Modifier.testTag` como `viewIdResourceName` usando
`testTagsAsResourceId`. O teste instrumentado confirmou que a Tela 1 expõe
`navgraph_continue_page_1` e a Tela 2 expõe `navgraph_continue_page_2`.

Isso fornece IDs estáveis para UiAutomator e para as asserções do BlindCheck,
mas não cria um “ID de foco” reconhecido pelo TalkBack. No emulador testado, o
cenário com IDs únicos teve exatamente o mesmo foco e a mesma sequência TTS do
baseline. O ID é útil para automação; não é um mecanismo confiável para mudar a
heurística de foco do leitor de tela.

## Deliberação

Rótulos diferentes não são uma solução para este caso. Eles existem somente
como grupo de controle para demonstrar que a ambiguidade desaparece quando a
identidade textual muda.

IDs por página e `key(page)` não alteraram o comportamento do TalkBack. O
`paneTitle` anunciou a mudança de contexto, mas manteve o foco em `Continuar`.
O único experimento que colocou o foco no primeiro item e produziu uma única
fala foi `ACTION_ACCESSIBILITY_FOCUS` aplicado ao título novo, sem enviar um
segundo evento sintético.

Esse resultado torna o foco imperativo um candidato de contorno para o projeto,
mas ainda não uma garantia geral: ele depende do timing entre Compose, a árvore
Android e a versão do leitor de tela. A adoção deve ficar protegida por testes
instrumentados e TTS spy, e ser validada em mais combinações de Android e
TalkBack antes de virar comportamento padrão.

## Regressão automatizada

O runner versionado executa essa matriz com:

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
