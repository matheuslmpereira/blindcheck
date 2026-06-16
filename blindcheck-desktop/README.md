# blindcheck-desktop

Aplicativo desktop (JVM/Compose Desktop) para controle remoto do dispositivo Android durante sessões de teste de acessibilidade manual. Dispara gestos via ADB sem precisar de teclado físico ou touch no dispositivo, e exibe em tempo real os anúncios do TalkBack.

---

## Requisitos

- ADB instalado e no PATH (ou `ANDROID_HOME` configurado)
- `blindcheck-tracking-app` instalado e o serviço de acessibilidade ativado no dispositivo
- Dispositivo conectado via USB ou emulador rodando

---

## Como executar

```bash
./gradlew :blindcheck-desktop:run
```

---

## Interface

### Status do dispositivo

Barra no topo mostrando o dispositivo conectado. Botão **Atualizar** para redetectar. Todos os botões ficam desabilitados enquanto não há dispositivo.

### Navegação TalkBack

| Botão | Teclado | Ação enviada | Equivale a |
|---|---|---|---|
| **← Anterior** | `←` | `ACTION_PREVIOUS` | Swipe esquerda |
| **Próximo →** | `→` | `ACTION_NEXT` | Swipe direita |
| **Ativar** | `Enter` / `Espaço` | `ACTION_ACTIVATE` | Duplo-toque |

### Scroll

| Botão | Teclado | Ação enviada | Equivale a |
|---|---|---|---|
| **↑ Scroll** | `↑` | `ACTION_SCROLL_FORWARD` | 2 dedos p/ cima |
| **↓ Scroll** | `↓` | `ACTION_SCROLL_BACKWARD` | 2 dedos p/ baixo |

### Sistema

| Botão | Teclado | Ação enviada | Equivale a |
|---|---|---|---|
| **Voltar** | `Esc` / `Backspace` | `ACTION_BACK` | Botão voltar |
| **Home** | — | `ACTION_HOME` | Tela inicial |
| **Recentes** | — | `ACTION_RECENTS` | Apps recentes |
| **⬆ Swipe** | — | `ACTION_SWIPE_UP` | Arrastar p/ cima |
| **⬇ Swipe** | — | `ACTION_SWIPE_DOWN` | Arrastar p/ baixo |

> **Dica:** Após abrir o app, o foco de teclado está no painel principal. Use as teclas de seta para navegar sem tirar a mão do teclado.

### Log de anúncios

Exibe em tempo real os anúncios aproximados inferidos pelo serviço de acessibilidade e, quando o engine "BlindCheck TTS capture" está selecionado como saída TTS do sistema, o texto enviado de fato ao `TextToSpeechService`. Atualiza a cada 500ms via polling de logcat.

O controle **TTS** no painel lateral alterna o engine TTS do Android entre:

* **BlindCheck**: captura o texto enviado ao `TextToSpeechService`;
* **Sistema**: volta para o engine TTS padrão do emulador/dispositivo e deixa de capturar TTS real.

Ao alternar o controle **TTS**, o desktop reinicia TalkBack + BlindCheck no emulador para evitar conexões TTS antigas do TalkBack e manter a captura estável.

| Indicador | Significado |
|---|---|
| ♿ (ícone acessibilidade, cinza) | Evento FOCUS — elemento que recebeu foco TalkBack |
| 🔊 (ícone volume, laranja) | Evento ANN — anúncio ou live region |
| 📱 (ícone smartphone, laranja) | Evento WIN — mudança de tela ou janela |
| TTS | Texto recebido pelo engine TTS controlado do BlindCheck |
| earcon | Feedback sonoro inferido, como limite de navegação |
| ✓ / ✗ | Ação enviada com sucesso / erro |

**Limpar log:** botão 🗑️ no canto superior direito do painel, ou `Delete` no teclado.

**Modo Logcat:** botão "Logcat" alterna para a view de linhas brutas do logcat (tag `BlindCheckAnnounce`), útil para debug.

---

## Como funciona

Cada botão executa:

```
adb shell am broadcast -p com.theustech.blindcheck_tracking_app -a <ACTION>
```

O broadcast é recebido pelo `RemoteActionReceiver` no dispositivo, que delega ao `TrackingAccessibilityService` para injetar o gesto físico correspondente.

O log de anúncios faz polling de:

```
adb logcat -d -s BlindCheckAnnounce:I
```

O serviço emite linhas com prefixos `FOCUS`, `ANN`, `WIN`, `TTS` e `EARCON` que o desktop parseia para exibir cada tipo com visual distinto.

O desktop mantém um conjunto de linhas de logcat já vistas para evitar repetir mensagens antigas no painel.

`EARCON` é inferido quando uma ação `next` ou `previous` não muda o foco depois de um timeout curto. Ele não é captura de áudio real.

---

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `ANDROID_HOME` | Localiza o ADB em `$ANDROID_HOME/platform-tools/adb` |

Se `ANDROID_HOME` não estiver definido, o app tenta encontrar o ADB no PATH.
