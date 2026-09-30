package com.dolemes.braingames.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dolemes.braingames.AppContainer
import com.dolemes.braingames.R
import com.dolemes.braingames.core.PlayerProgress
import com.dolemes.braingames.core.RoundEngine
import com.dolemes.braingames.core.RoundResult
import com.dolemes.braingames.game.MiniGame
import com.dolemes.braingames.game.MiniGameDefinition
import com.dolemes.braingames.game.RoundSession
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private enum class Phase { INTRO, COUNTDOWN, PLAYING, RESULTS }

private const val COUNTDOWN_STEP_MS = 800L

/**
 * Roda qualquer minijogo com o mesmo fluxo: instrução → 3-2-1 → rodada → resultado →
 * (intersticial, se a política deixar) → Home. Cuida de cronômetro, pausa (inclusive quando o
 * app vai para segundo plano), botão Voltar, tela sempre acesa, save e anúncios.
 */
@Composable
fun GameHostScreen(
    game: MiniGame,
    container: AppContainer,
    progress: PlayerProgress,
    onExit: () -> Unit,
) {
    val definition = game.definition
    val activity = LocalActivity.current
    val latestProgress by rememberUpdatedState(progress)
    var phase by remember { mutableStateOf(Phase.INTRO) }
    var session by remember { mutableStateOf<RoundSession?>(null) }
    var countdown by remember { mutableIntStateOf(3) }
    var previousBest by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf(false) }

    KeepScreenOn()

    fun leave() {
        if (leaving) return
        leaving = true
        onExit()
    }

    // Anúncio só aqui: depois do "Continuar" na tela de resultado.
    fun continueAfterResults() {
        if (leaving) return
        leaving = true
        val now = SystemClock.elapsedRealtime() / 1000.0
        val canShow = activity != null &&
            container.ads.isInterstitialReady &&
            container.adPolicy.shouldShowInterstitial(latestProgress.completedSessions, now)
        if (canShow && activity != null) {
            container.adPolicy.onInterstitialShown(now)
            container.ads.showInterstitial(activity) { onExit() }
        } else {
            onExit()
        }
    }

    if (phase == Phase.COUNTDOWN) {
        LaunchedEffect(Unit) {
            for (n in 3 downTo 1) {
                countdown = n
                delay(COUNTDOWN_STEP_MS)
            }
            val p = latestProgress
            val saved = p.game(definition.id)
            previousBest = saved?.bestScore ?: 0
            val timed = !(p.preferences.untimedMode && definition.supportsUntimedMode)
            session = RoundSession(
                engine = RoundEngine(
                    gameId = definition.id,
                    staircaseSettings = definition.staircase,
                    savedStaircase = saved?.staircase,
                    roundSeconds = definition.roundSeconds,
                    timed = timed,
                    supportsUntimedMode = definition.supportsUntimedMode,
                    untimedTrials = definition.untimedTrials,
                ),
                random = Random(System.nanoTime()),
            )
            phase = Phase.PLAYING
        }
    }

    val current = session
    if (phase == Phase.PLAYING && current != null) {
        // Relógio da rodada: avança a cada quadro; parado durante a pausa.
        LaunchedEffect(current) {
            var last = withFrameNanos { it }
            while (current.isRunning) {
                if (current.isPaused) {
                    snapshotFlow { current.isPaused }.first { !it }
                    last = withFrameNanos { it }
                    continue
                }
                val now = withFrameNanos { it }
                current.tick((now - last) / 1_000_000_000.0)
                last = now
            }
        }
        // Fim da rodada: salva e mostra o resultado (ou volta, se o jogador saiu no meio).
        LaunchedEffect(current.result) {
            val result = current.result ?: return@LaunchedEffect
            container.recordRound(result, current.finalStaircase())
            if (result.completed) phase = Phase.RESULTS else leave()
        }
        // Ligação, notificação, troca de app: pausa sem penalizar. Não retoma sozinho.
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { current.setPaused(true) }
    }

    BackHandler {
        when (phase) {
            Phase.INTRO, Phase.COUNTDOWN -> leave()
            Phase.PLAYING -> current?.let { it.setPaused(!it.isPaused) }
            Phase.RESULTS -> continueAfterResults()
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (phase) {
            Phase.INTRO -> IntroPanel(definition, onStart = { phase = Phase.COUNTDOWN }, onBack = { leave() })
            Phase.COUNTDOWN -> CountdownPanel(countdown)
            Phase.PLAYING -> if (current != null) {
                Column(Modifier.fillMaxSize()) {
                    Hud(
                        isTimed = current.isTimed,
                        secondsLeft = current.secondsLeft,
                        trialsLeft = current.trialsRemaining,
                        score = current.score,
                        onPause = { current.setPaused(true) },
                    )
                    game.Play(current, Modifier.weight(1f).fillMaxWidth())
                }
                if (current.isPaused && current.isRunning) {
                    PauseDialog(onResume = { current.setPaused(false) }, onQuit = { current.abort() })
                }
            }
            Phase.RESULTS -> current?.result?.let { result ->
                ResultsPanel(result, previousBest, onContinue = { continueAfterResults() })
            }
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun IntroPanel(
    definition: MiniGameDefinition,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(definition.nameRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        // Espaço para a demonstração animada (2–3 tentativas) descrita no GDD do jogo.
        Text(
            text = stringResource(definition.howToRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.game_start), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.game_back))
        }
    }
}

@Composable
private fun CountdownPanel(value: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(value.toString(), style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun Hud(isTimed: Boolean, secondsLeft: Int, trialsLeft: Int, score: Int, onPause: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (isTimed) stringResource(R.string.game_time_left, secondsLeft)
            else stringResource(R.string.game_trials_left, trialsLeft),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.game_score, score), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onPause, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.game_pause))
        }
    }
}

@Composable
private fun PauseDialog(onResume: () -> Unit, onQuit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(stringResource(R.string.game_paused_title)) },
        confirmButton = {
            Button(onClick = onResume, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.game_resume))
            }
        },
        dismissButton = {
            TextButton(onClick = onQuit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.game_quit))
            }
        },
    )
}

@Composable
fun ResultsPanel(
    result: RoundResult,
    previousBest: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.results_title), style = MaterialTheme.typography.headlineMedium)
        if (previousBest > 0 && result.score > previousBest) {
            Text(
                text = stringResource(R.string.results_new_record),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(stringResource(R.string.results_score, result.score), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.results_best, maxOf(previousBest, result.score)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.results_level, result.endLevel), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.results_accuracy, (result.accuracy * 100).roundToInt()),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 12.dp)) {
            Text(stringResource(R.string.results_continue), style = MaterialTheme.typography.titleMedium)
        }
    }
}
