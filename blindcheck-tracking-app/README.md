# blindcheck-tracking-app

Aplicativo Android para monitoramento do stream de eventos de acessibilidade em tempo real. Instalado no dispositivo junto ao app sendo testado, captura todos os eventos do sistema via `TrackingAccessibilityService` e os exibe em uma interface Compose.

---

## O que faz

- Exibe em tempo real todos os eventos de acessibilidade gerados por qualquer app no dispositivo
- Permite filtrar por pacote (app) e tipo de evento
- Mostra o estado do serviço de acessibilidade (ativo / inativo)
- Exporta o log de eventos como texto
- Controla gravação: pausar, retomar, limpar

---

## Como usar

### 1. Instalar o app no dispositivo

```bash
./gradlew :blindcheck-tracking-app:installDebug
```

### 2. Ativar o serviço de acessibilidade

Via makefile:
```bash
make enable-tracking
```

Ou manualmente: **Configurações → Acessibilidade → Serviços instalados → BlindCheck**.

### 3. Abrir o app e iniciar gravação

O app começa a capturar eventos de todos os apps assim que o serviço é ativado. Use o botão **●** para pausar/retomar.

### 4. Filtrar por app

No campo "Pacote", digite o ID do app sendo testado (ex.: `com.meuapp`) e toque em **Adicionar**.

---

## Interface

### Barra superior
- Indicador de status do serviço (vermelho se desativado)
- Indicador de gravação (● gravando / ○ pausado)

### Filtros
- **Pacote**: filtra eventos de apps específicos
- **Tipo de evento**: filtra por tipo (`TYPE_VIEW_ACCESSIBILITY_FOCUSED`, etc.)
- Chips removíveis com "Limpar todos"

### Lista de eventos
Cada linha mostra:
```
[timestamp] · [pacote] · [tipo de evento]
  Texto: [texto do nó]
  ContentDescription: [descrição]
  Estado: focused · clickable · editable · disabled
```

Ordenação alternável (mais novo / mais antigo primeiro).

### Ações
- **Pausar/Retomar**: congela ou retoma a captura
- **Resetar**: limpa todos os eventos gravados
- **Compartilhar**: exporta o log como texto plano

---

## Controle remoto via ADB

Com o serviço ativo, você pode disparar gestos remotamente:

```bash
make next        # swipe direita (próximo elemento TalkBack)
make previous    # swipe esquerda
make activate    # duplo-toque
make back        # voltar
make home        # tela inicial
make recents     # apps recentes
make swipe-up    # arrastar para cima
make swipe-down  # arrastar para baixo
make scroll-forward
make scroll-backward
```

---

## Dependências

```
blindcheck-tracking-app
    ├── blindcheck-tracker      (normalização de eventos, store)
    └── blindcheck-interactor   (serviço de acessibilidade, receiver)
```
