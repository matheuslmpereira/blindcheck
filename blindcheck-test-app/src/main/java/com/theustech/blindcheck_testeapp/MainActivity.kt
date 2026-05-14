package com.theustech.blindcheck_testeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theustech.blindcheck_testeapp.ui.theme.BlindchecktesteappTheme

private val deterministicFruits = listOf(
    Fruit("Banana", "Fruta amarela, doce e facil de descascar."),
    Fruit("Laranja", "Fruta citrica com gomos e bastante suco."),
    Fruit("Uva", "Fruta pequena que cresce em cachos."),
    Fruit("Abacaxi", "Fruta tropical com casca rigida e polpa doce."),
    Fruit("Manga", "Fruta macia, aromatica e suculenta."),
    Fruit("Pera", "Fruta clara, leve e com sabor suave."),
    Fruit("Melancia", "Fruta grande com polpa vermelha e muita agua."),
    Fruit("Morango", "Fruta vermelha pequena usada em sobremesas."),
    Fruit("Kiwi", "Fruta verde com sementes pequenas."),
    Fruit("Mamao", "Fruta macia comum no cafe da manha."),
    Fruit("Limao", "Fruta citrica usada para temperos e sucos."),
    Fruit("Caju", "Fruta tropical com castanha externa."),
)

data class Fruit(
    val name: String,
    val description: String,
)

private sealed interface MockupScreen {
    data object Login : MockupScreen
    data object FruitList : MockupScreen
    data class FruitDetail(val fruit: Fruit) : MockupScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlindchecktesteappTheme {
                BlindCheckMockupApp()
            }
        }
    }
}

@Composable
fun BlindCheckMockupApp() {
    var screen by remember { mutableStateOf<MockupScreen>(MockupScreen.Login) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (val currentScreen = screen) {
            MockupScreen.Login -> LoginScreen(
                modifier = Modifier.padding(innerPadding),
                onLoginSuccess = { screen = MockupScreen.FruitList },
            )

            MockupScreen.FruitList -> FruitListScreen(
                modifier = Modifier.padding(innerPadding),
                fruits = deterministicFruits,
                onFruitClick = { screen = MockupScreen.FruitDetail(it) },
            )

            is MockupScreen.FruitDetail -> FruitDetailScreen(
                modifier = Modifier.padding(innerPadding),
                fruit = currentScreen.fruit,
                onBack = { screen = MockupScreen.FruitList },
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val emailError = submitted && email.isBlank()
    val passwordError = submitted && password.isBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Acessar conta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("E-mail") },
            isError = emailError,
            supportingText = {
                if (emailError) {
                    Text("Informe o e-mail")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Senha") },
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text("Informe a senha")
                }
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                submitted = true
                if (email.isNotBlank() && password.isNotBlank()) {
                    onLoginSuccess()
                }
            },
        ) {
            Text("Entrar")
        }
    }
}

@Composable
fun FruitListScreen(
    fruits: List<Fruit>,
    onFruitClick: (Fruit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "Frutas",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(fruits) { fruit ->
                FruitListItem(
                    fruit = fruit,
                    onClick = { onFruitClick(fruit) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FruitListItem(
    fruit: Fruit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = fruit.name,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = fruit.description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun FruitDetailScreen(
    fruit: Fruit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Voltar",
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = Color(0xFFE5F3E8),
                    shape = RoundedCornerShape(8.dp),
                )
                .semantics {
                    contentDescription = "Imagem de ${fruit.name}"
                },
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = fruit.name.first().toString(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                )
                Text(
                    text = "Imagem de ${fruit.name}",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Text(
            text = fruit.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = fruit.description,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun BlindCheckMockupAppPreview() {
    BlindchecktesteappTheme {
        BlindCheckMockupApp()
    }
}
