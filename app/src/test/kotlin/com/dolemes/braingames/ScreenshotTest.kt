package com.dolemes.braingames

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dolemes.braingames.core.RoundResult
import com.dolemes.braingames.core.games.flanker.FlankerTrial
import com.dolemes.braingames.game.GameCatalog
import com.dolemes.braingames.games.flanker.FlankerGame
import com.dolemes.braingames.games.flanker.FlankerStimulus
import com.dolemes.braingames.ui.HomeScreen
import com.dolemes.braingames.ui.HomeUiState
import com.dolemes.braingames.ui.IntroPanel
import com.dolemes.braingames.ui.ResultsPanel
import com.dolemes.braingames.ui.WorkoutItem
import com.dolemes.braingames.ui.theme.BrainGamesTheme
import com.github.takahirom.roborazzi.captureRoboImage
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
        workout = listOf(WorkoutItem(FlankerGame.definition, done = true)),
        workoutComplete = true,
        streakDays = 3,
        gamesByDomain = GameCatalog.all.map { it.definition }.groupBy { it.domain }.toList(),
    )

    private val result = RoundResult("flanker", 180, 24, 19, 1, 7, 8, 60.0, 520.0, true)

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

    private fun capture(textScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            BrainGamesTheme(textScale = textScale) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
