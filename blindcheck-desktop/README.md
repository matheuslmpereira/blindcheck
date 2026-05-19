# blindcheck-desktop

Aplicativo desktop (JVM/Compose Desktop) para controle remoto do dispositivo Android durante sessões de teste de acessibilidade manual. Dispara gestos via ADB sem precisar de teclado físico ou touch no dispositivo.

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

| Botão | Ação enviada | Equivale a |
|---|---|---|
| **← Anterior** | `ACTION_PREVIOUS` | Swipe esquerda |
| **Próximo →** | `ACTION_NEXT` | Swipe direita |
| **Ativar** | `ACTION_ACTIVATE` | Duplo-toque |

### Scroll

| Botão | Ação enviada | Equivale a |
|---|---|---|
| **↑ Scroll** | `ACTION_SCROLL_FORWARD` | 2 dedos p/ cima |
| **↓ Scroll** | `ACTION_SCROLL_BACKWARD` | 2 dedos p/ baixo |

### Sistema

| Botão | Ação enviada | Equivale a |
|---|---|---|
| **Voltar** | `ACTION_BACK` | Botão voltar |
| **Home** | `ACTION_HOME` | Tela inicial |
| **Recentes** | `ACTION_RECENTS` | Apps recentes |
| **⬆ Swipe** | `ACTION_SWIPE_UP` | Arrastar p/ cima |
| **⬇ Swipe** | `ACTION_SWIPE_DOWN` | Arrastar p/ baixo |

### Log de ações

Exibe as últimas 10 ações disparadas com o resultado (sucesso/erro).

---

## Como funciona

Cada botão executa:

```
adb shell am broadcast -p com.theustech.blindcheck_tracking_app -a <ACTION>
```

O broadcast é recebido pelo `RemoteActionReceiver` no dispositivo, que delega ao `TrackingAccessibilityService` para injetar o gesto físico correspondente.

---

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `ANDROID_HOME` | Localiza o ADB em `$ANDROID_HOME/platform-tools/adb` |

Se `ANDROID_HOME` não estiver definido, o app tenta encontrar o ADB no PATH.
