package com.theustech.blindcheck_testeapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavBackStackEntry
import com.theustech.blindcheck_focus.resetAccessibilityFocusOnEnter
import com.theustech.blindcheck_testeapp.ui.theme.BlindchecktesteappTheme

internal val deterministicFruits = listOf(
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
    data class ThreeScreenScenario(val page: Int) : MockupScreen
    data object ThreeScreenNavGraph : MockupScreen
    data class ThreeScreenScenarioWithLabel(val page: Int) : MockupScreen
    data object ThreeScreenNavGraphWithLabel : MockupScreen
    data class ThreeScreenColorScenario(val page: Int) : MockupScreen
    data object ThreeScreenNavGraphColorScenario : MockupScreen
    data object ThreeScreenNavGraphFocusResetScenario : MockupScreen
    data object ThreeScreenNavGraphFocusResetWithLabel : MockupScreen
    data object ThreeScreenNavGraphFocusResetColorScenario : MockupScreen
    data class NavGraphApproachScenario(
        val approach: NavGraphAccessibilityApproach,
    ) : MockupScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialApproach = NavGraphAccessibilityApproach.fromArgument(
            intent.getStringExtra(EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH),
        )
        setContent {
            BlindchecktesteappTheme {
                BlindCheckMockupApp(initialNavGraphApproach = initialApproach)
            }
        }
    }
}

@Composable
fun BlindCheckMockupApp(
    initialNavGraphApproach: NavGraphAccessibilityApproach? = null,
) {
    var screen by remember(initialNavGraphApproach) {
        mutableStateOf<MockupScreen>(
            initialNavGraphApproach
                ?.let(MockupScreen::NavGraphApproachScenario)
                ?: MockupScreen.Login,
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (val currentScreen = screen) {
            MockupScreen.Login -> LoginScreen(
                modifier = Modifier.padding(innerPadding),
                onLoginSuccess = { screen = MockupScreen.FruitList },
                onStartThreeScreenScenario = { screen = MockupScreen.ThreeScreenScenario(page = 1) },
                onStartThreeScreenNavGraphScenario = { screen = MockupScreen.ThreeScreenNavGraph },
                onStartScenarioWithLabelOne = { screen = MockupScreen.ThreeScreenScenarioWithLabel(page = 1) },
                onStartNavGraphScenarioWithLabelTwo = { screen = MockupScreen.ThreeScreenNavGraphWithLabel },
                onStartColorScenario = { screen = MockupScreen.ThreeScreenColorScenario(page = 1) },
                onStartNavGraphColorScenario = { screen = MockupScreen.ThreeScreenNavGraphColorScenario },
                onStartNavGraphFocusResetScenario = { screen = MockupScreen.ThreeScreenNavGraphFocusResetScenario },
                onStartNavGraphFocusResetWithLabel = {
                    screen = MockupScreen.ThreeScreenNavGraphFocusResetWithLabel
                },
                onStartNavGraphFocusResetColorScenario = {
                    screen = MockupScreen.ThreeScreenNavGraphFocusResetColorScenario
                },
                onStartNavGraphApproach = { approach ->
                    screen = MockupScreen.NavGraphApproachScenario(approach)
                },
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

            is MockupScreen.ThreeScreenScenario -> ThreeScreenScenarioScreen(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                page = currentScreen.page,
                onContinue = {
                    screen = MockupScreen.ThreeScreenScenario(
                        page = if (currentScreen.page == 3) 1 else currentScreen.page + 1,
                    )
                },
            )

            MockupScreen.ThreeScreenNavGraph -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
            )

            is MockupScreen.ThreeScreenScenarioWithLabel -> ThreeScreenScenarioScreen(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                page = currentScreen.page,
                continueLabel = { page -> "continuar $page" },
                onContinue = {
                    screen = MockupScreen.ThreeScreenScenarioWithLabel(
                        page = if (currentScreen.page == 3) 1 else currentScreen.page + 1,
                    )
                },
            )

            MockupScreen.ThreeScreenNavGraphWithLabel -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                continueLabel = { page -> "continuar $page" },
            )

            is MockupScreen.ThreeScreenColorScenario -> ThreeScreenScenarioScreen(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                page = currentScreen.page,
                continueLabel = ::colorButtonLabel,
                onContinue = {
                    screen = MockupScreen.ThreeScreenColorScenario(
                        page = if (currentScreen.page == 3) 1 else currentScreen.page + 1,
                    )
                },
            )

            MockupScreen.ThreeScreenNavGraphColorScenario -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                continueLabel = ::colorButtonLabel,
            )

            MockupScreen.ThreeScreenNavGraphFocusResetScenario -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                accessibilityApproach = NavGraphAccessibilityApproach.LegacyCombinedReset,
            )

            MockupScreen.ThreeScreenNavGraphFocusResetWithLabel -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                accessibilityApproach = NavGraphAccessibilityApproach.LegacyCombinedReset,
                continueLabel = { page -> "continuar $page" },
            )

            MockupScreen.ThreeScreenNavGraphFocusResetColorScenario -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                accessibilityApproach = NavGraphAccessibilityApproach.LegacyCombinedReset,
                continueLabel = ::colorButtonLabel,
            )

            is MockupScreen.NavGraphApproachScenario -> ThreeScreenNavGraphScenario(
                modifier = Modifier.padding(innerPadding),
                onHome = { screen = MockupScreen.Login },
                accessibilityApproach = currentScreen.approach,
                showHomeAction = currentScreen.approach.rendersHomeAction,
            )
        }
    }

    if (screen is MockupScreen.FruitDetail) {
        BackHandler {
            screen = MockupScreen.FruitList
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onStartThreeScreenScenario: () -> Unit,
    onStartThreeScreenNavGraphScenario: () -> Unit,
    onStartScenarioWithLabelOne: () -> Unit,
    onStartNavGraphScenarioWithLabelTwo: () -> Unit,
    onStartColorScenario: () -> Unit,
    onStartNavGraphColorScenario: () -> Unit,
    onStartNavGraphFocusResetScenario: () -> Unit,
    onStartNavGraphFocusResetWithLabel: () -> Unit,
    onStartNavGraphFocusResetColorScenario: () -> Unit,
    onStartNavGraphApproach: (NavGraphAccessibilityApproach) -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var navigationTestCasesExpanded by rememberSaveable { mutableStateOf(false) }

    val emailError = submitted && email.isBlank()
    val passwordError = submitted && password.isBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        Text(
            text = "Comparação de reset de foco",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onStartNavGraphApproach(NavGraphAccessibilityApproach.ImperativeFocus) },
        ) {
            Text("Comparação: foco imperativo (método inicial)")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onStartNavGraphApproach(NavGraphAccessibilityApproach.AgnosticFocusReset) },
        ) {
            Text("Comparação: reset agnóstico pela lib")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navigationTestCasesExpanded = !navigationTestCasesExpanded },
        ) {
            Text(
                if (navigationTestCasesExpanded) {
                    "Ocultar casos de teste de navegação"
                } else {
                    "Mostrar casos de teste de navegação"
                },
            )
        }

        if (navigationTestCasesExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Casos de teste de navegação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartThreeScreenScenario,
                ) {
                    Text("Iniciar navegação por recomposição")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartThreeScreenNavGraphScenario,
                ) {
                    Text("Iniciar navegação por NavGraph")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartScenarioWithLabelOne,
                ) {
                    Text("Iniciar navegação numerada por recomposição")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNavGraphScenarioWithLabelTwo,
                ) {
                    Text("Iniciar navegação numerada por NavGraph")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartColorScenario,
                ) {
                    Text("Iniciar navegação por cores (recomposição)")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNavGraphColorScenario,
                ) {
                    Text("Iniciar navegação por cores (NavGraph)")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNavGraphFocusResetScenario,
                ) {
                    Text("Iniciar navegação por NavGraph com foco reiniciado")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNavGraphFocusResetWithLabel,
                ) {
                    Text("Iniciar navegação numerada por NavGraph com foco reiniciado")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNavGraphFocusResetColorScenario,
                ) {
                    Text("Iniciar navegação por cores (NavGraph com foco reiniciado)")
                }

                Text(
                    text = "Experimentos isolados de acessibilidade NavGraph",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                NavGraphAccessibilityApproach.experiments.forEach { approach ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onStartNavGraphApproach(approach) },
                    ) {
                        Text(approach.scenarioLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeScreenScenarioScreen(
    page: Int,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    continueLabel: (Int) -> String = { "Continuar" },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScenarioTopAppBar(onHome = onHome)

        Text(
            text = "Tela $page",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinue,
        ) {
            Text(continueLabel(page))
        }
    }
}

@Composable
fun ThreeScreenNavGraphScenario(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityApproach: NavGraphAccessibilityApproach = NavGraphAccessibilityApproach.Baseline,
    continueLabel: (Int) -> String = accessibilityApproach::continueLabel,
    showHomeAction: Boolean = true,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "nav_graph_screen_1",
        modifier = modifier,
    ) {
        composable("nav_graph_screen_1") { backStackEntry ->
            NavGraphScenarioDestination(
                backStackEntry = backStackEntry,
                page = 1,
                onHome = onHome,
                continueLabel = continueLabel,
                accessibilityApproach = accessibilityApproach,
                showHomeAction = showHomeAction,
                onContinue = { navController.navigate("nav_graph_screen_2") },
            )
        }
        composable("nav_graph_screen_2") { backStackEntry ->
            NavGraphScenarioDestination(
                backStackEntry = backStackEntry,
                page = 2,
                onHome = onHome,
                continueLabel = continueLabel,
                accessibilityApproach = accessibilityApproach,
                showHomeAction = showHomeAction,
                onContinue = { navController.navigate("nav_graph_screen_3") },
            )
        }
        composable("nav_graph_screen_3") { backStackEntry ->
            NavGraphScenarioDestination(
                backStackEntry = backStackEntry,
                page = 3,
                onHome = onHome,
                continueLabel = continueLabel,
                accessibilityApproach = accessibilityApproach,
                showHomeAction = showHomeAction,
                onContinue = {
                    navController.navigate("nav_graph_screen_1") {
                        popUpTo("nav_graph_screen_1") { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun NavGraphScenarioDestination(
    backStackEntry: NavBackStackEntry,
    page: Int,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    continueLabel: (Int) -> String,
    accessibilityApproach: NavGraphAccessibilityApproach,
    showHomeAction: Boolean,
) {
    if (accessibilityApproach.usesLibraryFocusReset) {
        // The library approach needs no per-screen infrastructure: the destination renders its
        // regular Compose content and hands the focus reset to the modifier, which resolves the
        // first accessible item from the semantics tree of this subtree alone.
        NavGraphScenarioContent(
            modifier = Modifier.resetAccessibilityFocusOnEnter(key = backStackEntry.id),
            page = page,
            onContinue = onContinue,
            onHome = onHome,
            continueLabel = continueLabel,
            accessibilityApproach = accessibilityApproach,
            showHomeAction = showHomeAction,
            registerInitialAccessibilityTarget = {},
        )
        return
    }

    if (accessibilityApproach.recreatesDestinationSemantics) {
        key(page) {
            NavGraphScenarioScreen(
                page = page,
                onContinue = onContinue,
                onHome = onHome,
                continueLabel = continueLabel,
                accessibilityApproach = accessibilityApproach,
                showHomeAction = showHomeAction,
            ).Render(backStackEntry)
        }
    } else {
        NavGraphScenarioScreen(
            page = page,
            onContinue = onContinue,
            onHome = onHome,
            continueLabel = continueLabel,
            accessibilityApproach = accessibilityApproach,
            showHomeAction = showHomeAction,
        ).Render(backStackEntry)
    }
}

private class NavGraphScenarioScreen(
    private val page: Int,
    private val onContinue: () -> Unit,
    private val onHome: () -> Unit,
    private val continueLabel: (Int) -> String,
    private val accessibilityApproach: NavGraphAccessibilityApproach,
    private val showHomeAction: Boolean,
) : Screen() {

    @Composable
    override fun Content(
        modifier: Modifier,
        registerInitialAccessibilityTarget: (View) -> Unit,
    ) {
        NavGraphScenarioContent(
            modifier = modifier,
            page = page,
            onContinue = onContinue,
            onHome = onHome,
            continueLabel = continueLabel,
            accessibilityApproach = accessibilityApproach,
            showHomeAction = showHomeAction,
            registerInitialAccessibilityTarget = registerInitialAccessibilityTarget,
        )
    }
}

@Composable
private fun NavGraphScenarioContent(
    modifier: Modifier,
    page: Int,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    continueLabel: (Int) -> String,
    accessibilityApproach: NavGraphAccessibilityApproach,
    showHomeAction: Boolean,
    registerInitialAccessibilityTarget: (View) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics {
                if (accessibilityApproach.announcesPaneTitle) {
                    isTraversalGroup = true
                    paneTitle = "Tela $page"
                }
                if (accessibilityApproach.exposesUniqueNodeIds) {
                    testTagsAsResourceId = true
                }
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHomeAction) {
            ScenarioTopAppBar(
                onHome = onHome,
                isFirstAccessibilityElement = accessibilityApproach.requestsImperativeAccessibilityFocus,
                requestPlatformAccessibilityFocus = accessibilityApproach.requestsImperativeAccessibilityFocus,
                onInitialAccessibilityTargetAvailable = registerInitialAccessibilityTarget,
            )
        }

        ScenarioTitle(
            page = page,
            requestPlatformAccessibilityFocus =
                accessibilityApproach.requestsImperativeAccessibilityFocus && !showHomeAction,
            onInitialAccessibilityTargetAvailable = registerInitialAccessibilityTarget,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    accessibilityApproach.continueNodeId(page)
                        ?.let { Modifier.testTag(it) }
                        ?: Modifier,
                ),
            onClick = onContinue,
        ) {
            Text(continueLabel(page))
        }
    }
}

@Composable
private fun ScenarioTitle(
    page: Int,
    requestPlatformAccessibilityFocus: Boolean,
    onInitialAccessibilityTargetAvailable: (View) -> Unit,
) {
    val title = "Tela $page"
    if (requestPlatformAccessibilityFocus) {
        key(page) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    TextView(context).apply {
                        text = title
                        textSize = 28f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                        onInitialAccessibilityTargetAvailable(this)
                    }
                },
            )
        }
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioTopAppBar(
    onHome: () -> Unit,
    isFirstAccessibilityElement: Boolean = false,
    requestPlatformAccessibilityFocus: Boolean = false,
    onInitialAccessibilityTargetAvailable: (View) -> Unit = {},
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            if (requestPlatformAccessibilityFocus) {
                AndroidView(
                    modifier = Modifier.size(48.dp),
                    factory = { context ->
                        ImageButton(context).apply {
                            contentDescription = "Ir para home"
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                            setOnClickListener { onHome() }
                            onInitialAccessibilityTargetAvailable(this)
                        }
                    },
                )
            } else {
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.semantics {
                        contentDescription = "Ir para home"
                        if (isFirstAccessibilityElement) {
                            traversalIndex = -1f
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

internal const val EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH =
    "com.theustech.blindcheck_testeapp.NAVGRAPH_ACCESSIBILITY_APPROACH"

private fun colorButtonLabel(page: Int): String = when (page) {
    1 -> "red"
    2 -> "blue"
    3 -> "green"
    else -> error("Unsupported scenario page: $page")
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
        TextButton(onClick = onBack) {
            Text("Voltar")
        }

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
