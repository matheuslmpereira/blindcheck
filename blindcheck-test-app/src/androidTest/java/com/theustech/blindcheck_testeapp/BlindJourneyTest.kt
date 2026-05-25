package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.theustech.blindcheck_testing.android.AndroidAccessibilitySetup
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Jornadas de usuário cego para o BlindCheck mock app.
 *
 * Cada teste simula exatamente o que um usuário utilizando TalkBack faria:
 *
 *   next()     → gesto de swipe direita: move o foco de acessibilidade para o próximo elemento
 *   assertFocused() → verifica o que o TalkBack anunciaria neste momento
 *   activate() → gesto de duplo-toque: ativa o elemento focado
 *   inputText()→ digita texto no campo focado (após ativar)
 *   back()     → pressiona voltar
 *
 * Nenhuma chamada de Compose UI (performClick, performTextInput) é usada aqui.
 * Toda interação passa pela camada de acessibilidade do Android.
 */
class BlindJourneyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val setup = AndroidAccessibilitySetup.create()
    private val driver = AndroidAccessibilityTestDriver.create()
    private val actions get() = driver.actions()

    @Before
    fun enableAccessibility() {
        setup.ensureAccessibilityEnabled()
        composeRule.waitForIdle()
    }

    // ─── Login: fluxo feliz ───────────────────────────────────────────────────

    /**
     * Jornada completa de login.
     *
     * Simula um usuário cego abrindo o app e navegando campo a campo com swipes
     * até submeter o formulário e chegar na tela de frutas.
     *
     *   Tela abre
     *     → [TalkBack anuncia: "Acessar conta"]
     *   swipe direita → foco no heading
     *     → [TalkBack anuncia: "Acessar conta"]
     *   swipe direita → foco no campo e-mail
     *     → [TalkBack anuncia: "E-mail, campo de texto editável"]
     *   duplo-toque → ativa o campo para edição
     *   digita e-mail
     *   swipe direita → foco no campo senha
     *     → [TalkBack anuncia: "Senha, campo de texto editável"]
     *   duplo-toque → ativa o campo
     *   digita senha
     *   swipe direita → foco no botão entrar
     *     → [TalkBack anuncia: "Entrar, botão"]
     *   duplo-toque → submete o formulário
     *     → [TalkBack anuncia: "Frutas" ao abrir a nova tela]
     */
    @Test
    fun loginJourney_swipeNavigation_logsInSuccessfully() = runTest {
        // Tela abre — heading já está acessível
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Acessar conta"))

        // → swipe direita: TalkBack anuncia o heading
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Acessar conta"))

        // → swipe direita: TalkBack anuncia "E-mail, campo editável"
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "E-mail", editable = true))
        actions.activate()                    // duplo-toque para editar
        actions.inputText("dev@example.com")

        // → swipe direita: TalkBack anuncia "Senha, campo editável"
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Senha", editable = true))
        actions.activate()
        actions.inputText("123456")

        // → swipe direita: TalkBack anuncia "Entrar, botão"
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Entrar", clickable = true))
        actions.activate()                    // duplo-toque para enviar

        composeRule.waitForIdle()

        // Nova tela: TalkBack anuncia ao entrar em Frutas
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    // ─── Login: fluxo de erro ─────────────────────────────────────────────────

    /**
     * Jornada de submissão sem preencher campos.
     *
     *   Navega até o botão com swipes
     *   duplo-toque → envia formulário vazio
     *     → [TalkBack anuncia as mensagens de erro nos campos]
     */
    @Test
    fun loginJourney_emptySubmit_errorsAreAnnouncedByScreenReader() = runTest {
        // Navega até o botão Entrar com swipes
        actions.next() // heading
        actions.next() // e-mail
        actions.next() // senha
        actions.next() // Entrar
        driver.assertFocused(FocusExpectation(textContains = "Entrar", clickable = true))

        // Duplo-toque no botão sem preencher os campos
        actions.activate()
        composeRule.waitForIdle()

        // TalkBack anuncia as mensagens de erro — acessíveis na árvore
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe o e-mail"))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe a senha"))
    }

    // ─── Fruit list: navegação após login ────────────────────────────────────

    /**
     * Após login, usuário navega pela lista de frutas com swipes.
     * Verifica que cada item é anunciado pelo TalkBack.
     */
    @Test
    fun fruitListJourney_swipeNavigation_fruitLabelsAreAccessible() = runTest {
        loginViaSwipeNavigation()

        // TalkBack anuncia o heading da lista
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Frutas"))

        // Swipe para os itens — cada fruta deve ser anunciável
        actions.next()
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Banana"))

        actions.next()
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Laranja"))
    }

    /**
     * Usuário navega até a Banana e abre o detalhe com duplo-toque.
     */
    @Test
    fun fruitDetailJourney_openAndReadDetail() = runTest {
        loginViaSwipeNavigation()

        // Navega até a Banana
        actions.next() // heading Frutas
        actions.next() // primeiro item (Banana)
        driver.assertFocused(FocusExpectation(textContains = "Banana"))

        // Duplo-toque: abre o detalhe
        actions.activate()
        composeRule.waitForIdle()

        // TalkBack anuncia os elementos da tela de detalhe
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Voltar", clickable = true))
        driver.assertCurrentWindowContains(
            FocusExpectation(contentDescriptionContains = "Imagem de Banana"),
        )
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta amarela, doce e facil de descascar."),
        )
    }

    /**
     * Usuário lê o detalhe e volta para a lista usando o botão Voltar.
     */
    @Test
    fun fruitDetailJourney_backButton_returnToList() = runTest {
        loginViaSwipeNavigation()

        // Abre detalhe da Banana
        actions.next() // heading Frutas
        actions.next() // item Banana
        actions.activate()
        composeRule.waitForIdle()

        // Foca o botão Voltar e ativa
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Voltar", clickable = true))
        actions.next() // Voltar
        driver.assertFocused(FocusExpectation(textContains = "Voltar", clickable = true))
        actions.activate()
        composeRule.waitForIdle()

        // TalkBack anuncia que voltou para a lista
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    // ─── Terceira fruta: jornada completa ────────────────────────────────────

    /**
     * Jornada: login → lista → terceira fruta (Uva) → leitura da descrição.
     *
     * Simula um usuário cego que:
     *   1. Faz login com swipes campo a campo
     *   2. Navega pela lista de frutas até chegar na terceira (Uva)
     *   3. Abre o detalhe com duplo-toque
     *   4. Percorre os elementos da tela de detalhe com swipes
     *   5. Ouve a descrição da fruta anunciada pelo TalkBack
     *
     * Frutas na lista (ordem determinística):
     *   1ª Banana · 2ª Laranja · 3ª Uva ← objetivo
     */
    @Test
    fun fruitListJourney_thirdFruit_openDetailAndReadDescription() = runTest {
        // ── 1. Login ──────────────────────────────────────────────────────────
        loginViaSwipeNavigation()

        // ── 2. Tela de lista: chega na terceira fruta com swipes ─────────────

        // → swipe: heading "Frutas"
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Frutas"))

        // → swipe: 1ª fruta — Banana
        //   [TalkBack anuncia: "Banana, Fruta amarela, doce e facil de descascar."]
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Banana"))

        // → swipe: 2ª fruta — Laranja
        //   [TalkBack anuncia: "Laranja, Fruta citrica com gomos e bastante suco."]
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Laranja"))

        // → swipe: 3ª fruta — Uva  ← chegamos ao objetivo
        //   [TalkBack anuncia: "Uva, Fruta pequena que cresce em cachos."]
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Uva"))

        // ── 3. Abre o detalhe da Uva ─────────────────────────────────────────
        //   [TalkBack anuncia: "Uva"] ao entrar na tela de detalhe
        actions.activate()
        composeRule.waitForIdle()

        // ── 4. Tela de detalhe: lê cada elemento com swipes ─────────────────

        // → swipe: botão Voltar
        //   [TalkBack anuncia: "Voltar, botão"]
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Voltar", clickable = true))

        // → swipe: imagem da fruta (Box com contentDescription explícito)
        //   [TalkBack anuncia: "Imagem de Uva"]
        actions.next()
        driver.assertFocused(FocusExpectation(contentDescriptionContains = "Imagem de Uva"))

        // → swipe: título da fruta
        //   [TalkBack anuncia: "Uva"]
        actions.next()
        driver.assertFocused(FocusExpectation(textContains = "Uva"))

        // → swipe: descrição da fruta  ← leitura que queríamos validar
        //   [TalkBack anuncia: "Fruta pequena que cresce em cachos."]
        actions.next()
        driver.assertFocused(
            FocusExpectation(textContains = "Fruta pequena que cresce em cachos."),
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun loginViaSwipeNavigation() {
        actions.next() // heading Acessar conta
        actions.next() // campo E-mail
        actions.activate()
        actions.inputText("dev@example.com")

        actions.next() // campo Senha
        actions.activate()
        actions.inputText("123456")

        actions.next() // botão Entrar
        actions.activate()
        composeRule.waitForIdle()
    }
}
