# Mockup Test App Specification

## Goal

Provide a controlled app used to validate BlindCheck features.

The mockup app must be deterministic because it is the reference target for agent-generated code, TDD, and end-to-end accessibility-flow validation.

## Screens

### Three-screen scenario

The mockup app also provides a small deterministic navigation scenario for validating
consecutive screen changes:

* `Tela 1` with a `Continuar` button;
* `Tela 2` with a `Continuar` button;
* `Tela 3` with a `Continuar` button.

The scenario is started from the login screen using `Iniciar cenário de 3 telas`.
Activating `Continuar` advances from Tela 1 to Tela 2 and from Tela 2 to Tela 3.
On Tela 3, it returns to Tela 1 so the flow can be repeated deterministically.

A second equivalent scenario is available through `Iniciar cenário NavGraph`.
This version uses AndroidX Navigation Compose with a `NavHost` and three named
destinations: `nav_graph_screen_1`, `nav_graph_screen_2`, and `nav_graph_screen_3`.

There are also two labeled variants of these scenarios. In both variants, the
button label follows the current page:

* `Iniciar navegação numerada por recomposição`: navigation by state, with `continuar 1`, `continuar 2`, and `continuar 3`;
* `Iniciar navegação numerada por NavGraph`: navigation by `NavGraph`, with `continuar 1`, `continuar 2`, and `continuar 3`.

Two additional scenarios use named color buttons:

* `Iniciar navegação por cores (recomposição)`: state navigation with `red`, `blue`, and `green`;
* `Iniciar navegação por cores (NavGraph)`: `NavGraph` navigation with `red`, `blue`, and `green`.

For comparison, the test app also provides three corrected NavGraph variants:

* `Iniciar navegação por NavGraph com foco reiniciado`;
* `Iniciar navegação numerada por NavGraph com foco reiniciado`;
* `Iniciar navegação por cores (NavGraph com foco reiniciado)`.

They preserve the destination structure and button labels of their corresponding
NavGraph scenario, but recreate each destination's semantic identity and mark
it as a new accessibility pane. The home action is explicitly first in the
screen-reader traversal order and is hosted by a native Android `View` that
requests accessibility focus after attachment. This imperative technique is an
experimental comparison only: Android documentation warns that apps should not
generally take accessibility focus from screen readers, because results vary by
device and reader version. The original NavGraph scenarios remain available as
regression comparisons.

The original and labeled comparison scenarios display a top app bar with an accessible
home icon labeled `Ir para home`. Activating it returns to the scenario selection screen.

On the selection screen, the only navigation shortcut shown by default is
`Iniciar solução NavGraph com foco acessível`. It launches the lifecycle-driven
`ImperativeFocus` implementation. All baseline, labeled, color, legacy, and
isolated comparison scenarios are retained under the collapsed
`Mostrar casos de teste de navegação` control, so they remain available for
regression investigation without competing with the recommended path.

### Isolated NavGraph accessibility approaches

The selection screen exposes a controlled matrix where every NavGraph scenario
changes one accessibility variable from the baseline:

* `Experimento NavGraph: baseline`;
* `Experimento NavGraph: rótulos únicos`;
* `Experimento NavGraph: IDs únicos por tela`;
* `Experimento NavGraph: semântica recriada`;
* `Experimento NavGraph: título de painel`;
* `Experimento NavGraph: foco imperativo`.

The final `Experimento NavGraph: rótulos únicos + título de painel` scenario
is retained as an experimental control. Different button labels are not an
acceptable fix for the repeated-label case under investigation.

The isolated experiment screens intentionally omit the home action. Their first
accessible item is the destination title (`Tela 1`, `Tela 2`, or `Tela 3`). This
prevents two identical `Ir para home` labels from obscuring whether a TTS request
belongs to the old or new destination.

The NavGraph destination in this matrix is implemented through the test app's
`Screen` base class. `Screen.Render(backStackEntry)` waits for the destination
to be `RESUMED`, for its root container to receive a layout, and for the screen
to register its initial native accessibility target. It then schedules one
`ACTION_ACCESSIBILITY_FOCUS` on the next Compose frame. This is lifecycle-driven
and does not use a fixed time delay. If the destination is discarded first, the
coroutine is cancelled and removes its native attach/layout listeners. The base
class does not know whether the target
is a title, button, field, or loading state; each concrete screen registers its
own first available accessible target.

The ID experiment keeps the visible and accessible name `Continuar`, but exports
`navgraph_continue_page_1`, `navgraph_continue_page_2`, and
`navgraph_continue_page_3` through `viewIdResourceName`. These are stable
automation handles, not a public Android contract for controlling TalkBack focus.

Every approach can be launched directly for external TTS testing:

```text
adb shell am start -n com.theustech.blindcheck_testeapp/.MainActivity \
  --es com.theustech.blindcheck_testeapp.NAVGRAPH_ACCESSIBILITY_APPROACH pane-title
```

Run `make navgraph-tts-matrix` to capture one pass of every approach, or
`NAVGRAPH_TTS_RUNS=3 make navgraph-tts-matrix` for repeated observations. Reports
are written under `blindcheck-test-app/build/reports/navgraph-tts-spy/`.

### NavGraph accessibility-focus regression test

`NavGraphAccessibilityFocusTest` is an opt-in instrumented test for a dedicated emulator
with TalkBack installed. It runs the focus-reset NavGraph scenario, activates `Continuar`
on Tela 1, and asserts that Tela 2 begins with accessibility focus on `Tela 2`.
Run it with the instrumentation argument `runTalkBackFocusTests=true`. It is skipped
when Android does not bind TalkBack in the instrumentation environment.

### Login

Elements:

* title: `Acessar conta`;
* email field;
* password field;
* submit button: `Entrar`.

Behaviors:

* empty submit shows email and password errors;
* valid submit navigates to fruit list.

Broken cases:

* visual error with missing accessible feedback;
* unlabeled button;
* duplicate label;
* noisy decorative element.

### Fruit List

Elements:

* title: `Frutas`;
* `LazyColumn` with many items;
* each fruit item is clickable.

Behaviors:

* selecting a fruit opens detail screen;
* list must support scroll navigation.

Broken cases:

* item without useful label;
* overly verbose item;
* scrollable container with bad focus sequence.

### Fruit Detail

Elements:

* back button;
* fruit image;
* fruit title;
* fruit description.

Behaviors:

* back returns to list.

Broken cases:

* decorative image exposed as meaningful;
* meaningful image missing description;
* back button without label.

## Test flows

Each flow should have an expected accessibility sequence documented in tests.

### Happy path

```text
Login → Fruit List → Fruit Detail → Back
```

### Error path

```text
Login → Submit empty → Error feedback
```

Expected result:

* the error feedback is observable in the accessibility event stream;
* the relevant field/error text can be asserted by the test library;
* intentionally broken cases must fail the relevant assertion.

### Long list path

```text
Login → Fruit List → Scroll → Select offscreen item
```

## Development rules

* Keep data deterministic.
* Avoid network dependencies.
* Avoid flaky animations in test paths.
* Keep text stable unless tests/docs are updated.
* Add broken cases deliberately and document the expected failure.
