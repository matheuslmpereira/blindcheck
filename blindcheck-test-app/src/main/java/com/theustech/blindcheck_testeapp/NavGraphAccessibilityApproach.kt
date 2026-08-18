package com.theustech.blindcheck_testeapp

enum class NavGraphAccessibilityApproach(
    val argumentValue: String,
    val scenarioLabel: String,
    val usesUniqueLabels: Boolean = false,
    val exposesUniqueNodeIds: Boolean = false,
    val recreatesDestinationSemantics: Boolean = false,
    val announcesPaneTitle: Boolean = false,
    val requestsImperativeAccessibilityFocus: Boolean = false,
    val usesLibraryFocusReset: Boolean = false,
    val isExperiment: Boolean = true,
    val isolatesSingleVariable: Boolean = true,
) {
    Baseline(
        argumentValue = "baseline",
        scenarioLabel = "Experimento NavGraph: baseline",
    ),
    UniqueLabels(
        argumentValue = "unique-labels",
        scenarioLabel = "Experimento NavGraph: rótulos únicos",
        usesUniqueLabels = true,
    ),
    UniqueNodeIds(
        argumentValue = "unique-node-ids",
        scenarioLabel = "Experimento NavGraph: IDs únicos por tela",
        exposesUniqueNodeIds = true,
    ),
    RecreatedSemantics(
        argumentValue = "recreated-semantics",
        scenarioLabel = "Experimento NavGraph: semântica recriada",
        recreatesDestinationSemantics = true,
    ),
    PaneTitle(
        argumentValue = "pane-title",
        scenarioLabel = "Experimento NavGraph: título de painel",
        announcesPaneTitle = true,
    ),
    ImperativeFocus(
        argumentValue = "imperative-focus",
        scenarioLabel = "Experimento NavGraph: foco imperativo",
        requestsImperativeAccessibilityFocus = true,
    ),
    AgnosticFocusReset(
        argumentValue = "agnostic-focus-reset",
        scenarioLabel = "Experimento NavGraph: reset agnóstico pela lib",
        usesLibraryFocusReset = true,
    ),
    UniqueLabelsWithPaneTitle(
        argumentValue = "unique-labels-pane-title",
        scenarioLabel = "Experimento NavGraph: rótulos únicos + título de painel",
        usesUniqueLabels = true,
        announcesPaneTitle = true,
        isolatesSingleVariable = false,
    ),
    LegacyCombinedReset(
        argumentValue = "legacy-combined-reset",
        scenarioLabel = "NavGraph: reset combinado legado",
        recreatesDestinationSemantics = true,
        announcesPaneTitle = true,
        requestsImperativeAccessibilityFocus = true,
        isExperiment = false,
        isolatesSingleVariable = false,
    ),
    ;

    fun continueLabel(page: Int): String {
        requireSupportedPage(page)
        return if (usesUniqueLabels) "continuar $page" else "Continuar"
    }

    fun continueNodeId(page: Int): String? {
        requireSupportedPage(page)
        return if (exposesUniqueNodeIds) "navgraph_continue_page_$page" else null
    }

    companion object {
        val experiments: List<NavGraphAccessibilityApproach> = entries.filter { it.isExperiment }

        fun fromArgument(value: String?): NavGraphAccessibilityApproach? =
            entries.firstOrNull { it.argumentValue == value }
    }
}

private fun requireSupportedPage(page: Int) {
    require(page in 1..3) { "Unsupported scenario page: $page" }
}
