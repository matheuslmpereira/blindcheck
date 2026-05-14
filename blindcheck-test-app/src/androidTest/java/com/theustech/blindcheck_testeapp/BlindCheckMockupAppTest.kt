package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
    fun fruitItemClick_opensFruitDetail() {
        login()

        composeRule.onNodeWithText("Banana").performClick()

        composeRule.onNodeWithText("Voltar").assertIsDisplayed()
        composeRule.onNodeWithText("Imagem de Banana").assertIsDisplayed()
        composeRule.onNodeWithText("Fruta amarela, doce e facil de descascar.").assertIsDisplayed()
    }

    private fun login() {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("123456")
        composeRule.onNodeWithText("Entrar").performClick()
    }
}
