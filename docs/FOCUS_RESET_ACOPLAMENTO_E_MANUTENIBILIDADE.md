# Reset de foco entre destinos: o que foi medido, acoplamento e manutenibilidade

Documento de decisão. Reúne o que a matriz controlada de TTS conseguiu aferir
sobre cada mecanismo de reset de foco, quanto cada um acopla à tela e ao
Compose, e o que cada um custa para manter.

A investigação que originou os experimentos está em
[ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md](ANDROID_NAVGRAPH_TALKBACK_FOCUS_TTS_SPY.md).
Aqui não se repete a narrativa: este documento é a comparação.

## O problema, em uma frase

Ao trocar de destino no Navigation Compose, o Android mantém o foco de
acessibilidade no nó que considera equivalente ao anterior; quando duas telas
expõem a mesma ação — `Continuar` em cada etapa — a pessoa ouve o mesmo rótulo
outra vez e nada comunica que a tela mudou.

O registro público mais próximo é
[b/272065229](https://issuetracker.google.com/issues/272065229), aberto e sem
resposta. Não existe API pública para mover foco de acessibilidade:
`FocusRequester` move foco de teclado.

## Ambiente da medição

| Item | Valor |
| --- | --- |
| Dispositivo | emulador `Medium_Phone`, `sdk_gphone64_arm64` |
| Android | 16 (API 36) |
| TalkBack | Google TalkBack `16.0.0.738667889` |
| Engine TTS | `com.theustech.blindcheck_tracking_app` (spy do repositório) |
| Data | 19–20 de agosto de 2026 |
| Execuções | 3 por abordagem, mais 1 execução com `retire-leaving-screen` |

O spy registra o texto entregue ao engine TTS selecionado. Ele não captura
áudio, earcons, nem a fala se o TalkBack estiver usando outro engine.

## O que foi aferido

Sequência falada e foco final imediatamente após ativar `Continuar` na Tela 1.
Resultado idêntico nas três execuções de cada abordagem.

| Abordagem | Foco final | TTS após ativar | Resolve? |
| --- | --- | --- | --- |
| `baseline` | `Continuar` | `Continuar → Button → Double-tap…` | não — é o bug |
| `unique-node-ids` | `Continuar` | igual ao baseline | não |
| `recreated-semantics` | `Continuar` | igual ao baseline | não |
| `pane-title` | `Continuar` | `Tela 2 → Continuar → Button → …` | parcial — anuncia, não move |
| `unique-labels` (controle) | `Tela 2` | `Tela 2` | não é solução: muda o rótulo |
| `imperative-focus` | `Tela 2` | `Tela 2` | sim |
| `agnostic-focus-reset` | `Tela 2` | `Tela 2` | sim |
| `semantics-focus-flag` | `Tela 2` | `Tela 2` | sim |
| `semantics-focus-root` | `Tela 2` | `Tela 2` | sim |
| `focus-anchor` | `Tela 2` | `Tela 2` | sim, com nó extra |
| `unique-labels-pane-title` (controle) | `Tela 2` | `Tela 2 → Tela 2` | duplica a fala |
| `legacy-combined-reset` | `Ir para home` | `Tela 2 → Ir para home → Button → …` | não — alvo errado |
| `retire-leaving-screen` | — | `UNREACHABLE` | não aferido |

Três achados que não eram esperados:

1. **Marcar o raiz do destino basta.** `semantics-focus-root` aplica a flag
   `focused` no container-raiz — um nó em que nenhum leitor de tela para — e o
   foco acaba no primeiro descendente focável. Não é preciso descobrir qual
   composable é o primeiro.
2. **A combinação que o app entregava não resolvia o caso.**
   `legacy-combined-reset` reinicia o foco no botão Home, cujo rótulo é idêntico
   nas três telas. O reset acontece; a ambiguidade permanece.
3. **`retire-leaving-screen` não pôde ser dirigido.** O runner não consegue nem
   focar o botão da Tela 1 nesse cenário. Pode ser o roteiro ou pode ser o
   cenário deixando conteúdo ilegível para o leitor — está em aberto.

## Acoplamento

Três eixos independentes. Um mecanismo pode ser barato num e caro noutro.

| Mecanismo | Precisa saber o conteúdo da tela? | Exige mudança na tela | Acoplamento ao Compose |
| --- | --- | --- | --- |
| `imperative-focus` (`Screen`) | **sim** — a tela indica o primeiro alvo | herdar classe base, propagar `modifier`, registrar alvo, embrulhar o item em `AndroidView` | baixo — só `View` e ciclo de vida |
| `agnostic-focus-reset` (lib atual) | não | um modifier no raiz | **alto** — lê a árvore semântica via `RootForTest` |
| `semantics-focus-flag` | **sim** — a flag vai no primeiro item | um estado e um modifier no item | baixo — `semantics { focused }` |
| `semantics-focus-root` | não | um modifier no raiz | baixo — `semantics { focused }` |
| `focus-anchor` | não | um modifier no raiz | baixo — mas injeta um nó na ordem de leitura |
| `pane-title` | não | um modifier no raiz | baixo — API que a plataforma reconhece |

O ponto que a medição resolveu: **agnosticismo não exige leitura da árvore
semântica.** `semantics-focus-root` é tão agnóstico quanto a lib atual e não usa
`RootForTest`.

## Manutenibilidade

| | `imperative-focus` | `agnostic-focus-reset` | `semantics-focus-root` |
| --- | --- | --- | --- |
| Linhas de produção | 159 | 402 | ~20 (estimativa) |
| APIs Compose distintas | 8 | 15 | 3 |
| API deprecada ou experimental | nenhuma | nenhuma | nenhuma |
| API sensível a upgrade | nenhuma | `RootForTest` | nenhuma |
| Pontos de falha silenciosa | 3 (herança, `modifier`, registro) | 1 (modifier ausente) | 1 (modifier ausente) |
| Comportamento na rotação | rouba o foco de volta (`remember`) | preservado (`rememberSaveable`) | depende da implementação |
| Custo por tela adotante | alto | uma linha | uma linha |

Sobre `RootForTest`: é API pública e estável de `androidx.compose.ui:ui`, sem
`@Deprecated`, `@RestrictTo` ou opt-in experimental, e vive no artefato de
produção. Apesar disso é o único ponto do módulo que pode mudar num upgrade do
Compose, e não existe caminho público alternativo para ler a árvore semântica
de dentro do app. A degradação é segura — o cast é `as?`, então o reset deixa de
rodar e a tela mantém o comportamento padrão da plataforma —, e há teste
instrumentado que quebra o build se isso acontecer. Ainda assim, é uma
justificativa a sustentar em revisão de arquitetura que
`semantics-focus-root` simplesmente não precisa dar.

### O que quebra num upgrade

| Mecanismo | O que observar |
| --- | --- |
| `imperative-focus` | nada específico do Compose; muda se o comportamento de `ACTION_ACCESSIBILITY_FOCUS` mudar |
| `agnostic-focus-reset` | `RootForTest`, ordenação da árvore, nomes de propriedades semânticas |
| `semantics-focus-root` | comportamento do delegate de acessibilidade ao receber `focused` num container |

Nenhum dos três tem garantia contratual: todos dependem de comportamento
observado do TalkBack, não de API documentada para mover foco. É por isso que a
matriz de TTS é a proteção, e não o compilador.

## Leitura para adoção

O alvo de qualquer reset é **o primeiro item do destino**. No cenário real do
app — `Iniciar navegação por NavGraph`, com o app bar — esse primeiro item é
`Ir para home`, idêntico nas três telas. Reset sozinho não resolve esse caso:
ou o app bar fica fora do escopo do reset, ou entra `paneTitle` para nomear a
tela. `pane-title` isolado anuncia a troca e não move o foco; os dois juntos
ainda não foram medidos como cenário próprio.

Ordem de preferência sustentada pelo que foi medido:

1. `semantics-focus-root` — mesmo resultado, menor acoplamento, menor superfície
   de API;
2. `agnostic-focus-reset` — mesmo resultado, resolve o alvo explicitamente, ao
   custo de `RootForTest` e de 20× mais código;
3. `imperative-focus` — mesmo resultado, sem API sensível, ao custo de contrato
   por herança e `AndroidView` em cada tela;
4. `focus-anchor` — funciona, mas inventa um nó na ordem de leitura sem
   necessidade, já que a flag no raiz entrega o mesmo.

## Limitações

* Uma combinação de Android e TalkBack. Nada aqui é evidência de comportamento
  em outras versões ou fabricantes.
* O `summary.tsv` não registra versão de Android, versão do TalkBack nem estado
  de acessibilidade do dispositivo. Sem isso, comparar execuções feitas em
  momentos diferentes é frágil — vale gravar esse estado junto da medição.
* `retire-leaving-screen` continua sem medida.
* As linhas de produção contadas incluem KDoc e o parâmetro de estratégia
  introduzido depois; a estimativa de `semantics-focus-root` é de uma
  implementação equivalente à API pública atual, ainda não escrita.
