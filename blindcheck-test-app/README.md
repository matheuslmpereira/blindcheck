# blindcheck-test-app

App Android de exemplo com fluxos determinísticos para servir como alvo dos testes de acessibilidade do BlindCheck. Não é um app real — é um cenário controlado para desenvolvimento e validação da lib.

---

## Fluxos disponíveis

### Login (`LoginScreen`)

Tela de formulário com validação.

| Elemento | Tipo | Texto / Label |
|---|---|---|
| Heading | Não clicável | "Acessar conta" |
| Campo e-mail | Editável | "E-mail" |
| Campo senha | Editável, texto oculto | "Senha" |
| Botão submit | Clicável | "Entrar" |
| Erro e-mail | Texto de erro | "Informe o e-mail" |
| Erro senha | Texto de erro | "Informe a senha" |

**Comportamento:** botão "Entrar" habilitado somente com ambos os campos preenchidos. Erros exibidos ao submeter vazio.

**Credenciais válidas:** qualquer valor não-vazio nos dois campos.

### Lista de frutas (`FruitListScreen`)

Exibida após login bem-sucedido.

| Elemento | Tipo | Texto |
|---|---|---|
| Heading | Não clicável | "Frutas" |
| Item de fruta | Clicável | nome + descrição |

**Frutas disponíveis:** Banana, Laranja, Uva, Abacaxi, Manga, Melancia, Morango, Pêssego, Kiwi, Coco, Limão, Maçã.

### Detalhe da fruta (`FruitDetailScreen`)

Exibida ao ativar um item da lista.

| Elemento | Tipo | Conteúdo |
|---|---|---|
| Botão voltar | Clicável | "Voltar" |
| Imagem | Não clicável | contentDescription: "Imagem de [nome]" |
| Nome | Texto | nome da fruta |
| Descrição | Texto | descrição da fruta |

---

## Usando nos testes

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<MainActivity>()
```

O `blindcheck-testing` está disponível como dependência de `androidTest`.

### Sequência de elementos na tela de login

```
Acessar conta (heading)
    ↓
E-mail (editável)
    ↓
Senha (editável)
    ↓
Entrar (botão)
```

### Package ID

`com.theustech.blindcheck_testeapp`

---

## Exemplos de testes existentes

| Arquivo | O que testa |
|---|---|
| `BlindJourneyTest` | Jornadas completas via swipe navigation |
| `BlindUserFlowTest` | Verificações estáticas de acessibilidade |
| `BlindCheckMockupAppTest` | Testes de UI Compose convencionais (controle) |
| `BlindCheckTestingIntegrationTest` | Integração do driver com o app |
