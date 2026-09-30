package com.dolemes.braingames

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dolemes.braingames.core.RoundResult
import com.dolemes.braingames.core.games.corsi.CorsiRules
import com.dolemes.braingames.core.games.flanker.FlankerTrial
import com.dolemes.braingames.core.games.quickmath.MathProblem
import com.dolemes.braingames.game.GameCatalog
import com.dolemes.braingames.games.corsi.CorsiField
import com.dolemes.braingames.games.corsi.CorsiGame
import com.dolemes.braingames.games.corsi.CorsiStatus
import com.dolemes.braingames.games.flanker.FlankerGame
import com.dolemes.braingames.games.flanker.FlankerStimulus
import com.dolemes.braingames.games.quickmath.QuickMathGame
import com.dolemes.braingames.games.quickmath.QuickMathStimulus
import com.dolemes.braingames.ui.HomeScreen
import com.dolemes.braingames.ui.HomeUiState
import com.dolemes.braingames.ui.IntroPanel
import com.dolemes.braingames.ui.ResultsPanel
import com.dolemes.braingames.ui.WorkoutItem
import com.dolemes.braingames.ui.theme.BrainGamesTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de tela geradas no computador, sem celular: `./gradlew :app:recordRoborazziDebug`.
 * As imagens vão para app/build/outputs/roborazzi/ e o CI as publica como artefato, para
 * conferir layout, idioma e texto grande sem abrir o app.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val home = HomeUiState(
        workout = listOf(
            WorkoutItem(CorsiGame.definition, done = true),
            WorkoutItem(FlankerGame.definition, done = true),
            WorkoutItem(QuickMathGame.definition, done = false),
        ),
        workoutComplete = false,
        streakDays = 3,
        gamesByDomain = GameCatalog.all.map { it.definition }.groupBy { it.domain }.toList(),
    )

    private val result = RoundResult("flanker", 180, 24, 19, 1, 7, 8, 60.0, 520.0, true)

    private val corsiResult = RoundResult(
        "corsi", 240, 12, 9, 1, 8, 9, 90.0, null, true, metrics = mapOf("span" to 6.0),
    )

    // Campo de 360 × 480 dp com blocos de 72 dp (o que blockSizeFor escolhe para esse campo).
    private val corsiPositions = CorsiRules.layout(Random(3), 360f, 480f, 72f)

    @Test fun home_en() = capture { HomeScreen(home, onPlay = {}, onSettings = {}) }

    // "+pt-rBR" soma o idioma às qualificações da classe (tamanho e densidade da tela).
    @Test @Config(qualifiers = "+pt-rBR")
    fun home_pt() = capture { HomeScreen(home, onPlay = {}, onSettings = {}) }

    @Test @Config(qualifiers = "+pt-rBR")
    fun home_pt_largeText() = capture(textScale = 1.6f) { HomeScreen(home, onPlay = {}, onSettings = {}) }

    @Test @Config(qualifiers = "+pt-rBR")
    fun intro_pt() = capture { IntroPanel(FlankerGame.definition, onStart = {}, onBack = {}) }

    @Test fun flanker_incongruent_wrongFeedback() = capture {
        FlankerStimulus(FlankerTrial(targetRight = true, congruent = false), feedback = false)
    }

    @Test @Config(qualifiers = "+pt-rBR")
    fun results_pt_largeText() = capture(textScale = 1.6f) { ResultsPanel(result, previousBest = 150, onContinue = {}) }

    @Test @Config(qualifiers = "+pt-rBR")
    fun intro_corsi_pt() = capture { IntroPanel(CorsiGame.definition, onStart = {}, onBack = {}) }

    @Test @Config(qualifiers = "+pt-rBR")
    fun corsi_lit_pt() = capture { CorsiScene(lit = 4, order = null, feedback = null, yourTurn = false) }

    // Depois de um erro: ✗ e a ordem certa numerada nos blocos, com texto grande.
    @Test @Config(qualifiers = "+pt-rBR")
    fun corsi_error_reveal_pt_largeText() = capture(textScale = 1.6f) {
        CorsiScene(lit = null, order = listOf(6, 2, 8, 0, 4), feedback = false, yourTurn = false)
    }

    @Test @Config(qualifiers = "+pt-rBR")
    fun intro_quickMath_pt() = capture { IntroPanel(QuickMathGame.definition, onStart = {}, onBack = {}) }

    @Test @Config(qualifiers = "+pt-rBR")
    fun quickMath_pt() = capture {
        QuickMathStimulus(
            problem = MathProblem("47 + 6", 53, listOf(43, 53, 57, 59)),
            answered = null, feedback = null, revealCorrect = false, onAnswer = {},
        )
    }

    // Erro: a opção tocada ganha ✗ e a certa ganha ✓ (nunca só a cor).
    @Test @Config(qualifiers = "+pt-rBR")
    fun quickMath_wrongAnswer_pt() = capture {
        QuickMathStimulus(
            problem = MathProblem("12 × 4 − 18", 30, listOf(66, 30, 40, 32)),
            answered = 0, feedback = false, revealCorrect = true, onAnswer = {},
        )
    }

    // Pior caso de largura: conta de 11 caracteres e opções de 3 dígitos com texto 1,6.
    @Test @Config(qualifiers = "+pt-rBR")
    fun quickMath_pt_largeText() = capture(textScale = 1.6f) {
        QuickMathStimulus(
            problem = MathProblem("25 × 9 + 41", 266, listOf(662, 256, 266, 268)),
            answered = 2, feedback = true, revealCorrect = false, onAnswer = {},
        )
    }

    @Test @Config(qualifiers = "+pt-rBR")
    fun results_corsi_pt_largeText() = capture(textScale = 1.6f) {
        ResultsPanel(corsiResult, previousBest = 300, onContinue = {}, metrics = CorsiGame.definition.resultMetrics)
    }

    @Test @Config(qualifiers = "+pt-rBR")
    fun results_quickMath_pt() = capture {
        ResultsPanel(
            RoundResult("quick-math", 310, 26, 22, 3, 9, 9, 90.0, 1800.0, true),
            previousBest = 250,
            onContinue = {},
            metrics = QuickMathGame.definition.resultMetrics,
        )
    }

    @Composable
    private fun CorsiScene(lit: Int?, order: List<Int>?, feedback: Boolean?, yourTurn: Boolean) {
        Column(Modifier.fillMaxSize()) {
            CorsiStatus(yourTurn = yourTurn, feedback = feedback)
            CorsiField(
                positions = corsiPositions,
                blockDp = 72f,
                lit = lit,
                tapped = null,
                order = order,
                onTap = {},
                modifier = Modifier.padding(16.dp).size(360.dp, 480.dp),
            )
        }
    }

    private fun capture(textScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            BrainGamesTheme(textScale = textScale) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
