package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class BlindCheckMockupAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_showsStableTexts() {
        composeRule.onNodeWithText("Acessar conta").assertIsDisplayed()
        composeRule.onNodeWithText("E-mail").assertIsDisplayed()
        composeRule.onNodeWithText("Senha").assertIsDisplayed()
        composeRule.onNodeWithText("Entrar").assertIsDisplayed()
    }

    @Test
    fun emptySubmit_showsAccessibleErrorTexts() {
        composeRule.onNodeWithText("Entrar").performClick()

        composeRule.onNodeWithText("Informe o e-mail").assertIsDisplayed()
        composeRule.onNodeWithText("Informe a senha").assertIsDisplayed()
    }

    @Test
    fun validLogin_navigatesToFruitList() {
        login()

        composeRule.onNodeWithText("Frutas").assertIsDisplayed()
        composeRule.onNodeWithText("Banana").assertIsDisplayed()
        composeRule.onNodeWithText("Laranja").assertIsDisplayed()
    }

    @Test
    fun navigationTestsAreCollapsedAndTheDefinitiveSolutionIsAvailableImmediately() {
        composeRule.onNodeWithText("Iniciar solução NavGraph com foco acessível").assertIsDisplayed()
        composeRule.onAllNodesWithText("Iniciar navegação por recomposição").assertCountEquals(0)

        openNavigationTestCases()

        composeRule.onNodeWithText("Iniciar navegação por recomposição").assertIsDisplayed()
        composeRule.onAllNodesWithText("Iniciar navegação por NavGraph com foco reiniciado")
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("Experimento NavGraph: foco imperativo").assertCountEquals(1)
    }

    @Test
    fun definitiveNavGraphSolution_startsWithoutOpeningTheTestCases() {
        composeRule.onNodeWithText("Iniciar solução NavGraph com foco acessível").performClick()

        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Ir para home").assertCountEquals(0)
    }

    @Test
    fun fruitItemClick_opensFruitDetail() {
        login()

        composeRule.onNodeWithText("Banana").performClick()

        composeRule.onNodeWithText("Voltar").assertIsDisplayed()
        composeRule.onNodeWithText("Imagem de Banana").assertIsDisplayed()
        composeRule.onNodeWithText("Fruta amarela, doce e facil de descascar.").assertIsDisplayed()
    }

    @Test
    fun threeScreenScenario_continueNavigatesThroughAllScreens() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por recomposição").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
    }

    @Test
    fun threeScreenScenario_lastContinue_restartsAtFirstScreen() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por recomposição").performClick()
        repeat(2) { composeRule.onNodeWithText("Continuar").performClick() }

        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
    }

    @Test
    fun navGraphScenario_continueNavigatesThroughAllScreens() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por NavGraph").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
    }

    @Test
    fun labeledStateScenario_usesPageNumberedContinueButtons() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação numerada por recomposição").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 1").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 2").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 3").assertIsDisplayed()
    }

    @Test
    fun labeledNavGraphScenario_usesPageNumberedContinueButtons() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação numerada por NavGraph").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 1").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 2").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
        composeRule.onNodeWithText("continuar 3").assertIsDisplayed()
    }

    @Test
    fun colorStateScenario_usesRedBlueGreenButtons() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por cores (recomposição)").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("red").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("blue").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
        composeRule.onNodeWithText("green").assertIsDisplayed()
    }

    @Test
    fun colorNavGraphScenario_usesRedBlueGreenButtons() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por cores (NavGraph)").performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()
        composeRule.onNodeWithText("red").performClick()
        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("blue").performClick()
        composeRule.onNodeWithText("Tela 3").assertIsDisplayed()
        composeRule.onNodeWithText("green").assertIsDisplayed()
    }

    @Test
    fun recompositionScenario_homeIcon_returnsToScenarioSelection() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por recomposição").performClick()

        composeRule.onNodeWithContentDescription("Ir para home").performClick()

        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por recomposição").assertIsDisplayed()
    }

    @Test
    fun navGraphScenario_homeIcon_returnsToScenarioSelection() {
        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por NavGraph").performClick()

        composeRule.onNodeWithContentDescription("Ir para home").performClick()

        openNavigationTestCases()
        composeRule.onNodeWithText("Iniciar navegação por NavGraph").assertIsDisplayed()
    }

    private fun login() {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("123456")
        composeRule.onNodeWithText("Entrar").performClick()
    }

    private fun openNavigationTestCases() {
        composeRule.onNodeWithText("Mostrar casos de teste de navegação").performClick()
    }

}
