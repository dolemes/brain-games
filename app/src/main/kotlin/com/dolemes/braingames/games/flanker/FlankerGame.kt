package com.dolemes.braingames.games.flanker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dolemes.braingames.R
import com.dolemes.braingames.core.CognitiveDomain
import com.dolemes.braingames.core.StaircaseSettings
import com.dolemes.braingames.core.games.flanker.FlankerRules
import com.dolemes.braingames.core.games.flanker.FlankerTrial
import com.dolemes.braingames.game.MiniGame
import com.dolemes.braingames.game.MiniGameDefinition
import com.dolemes.braingames.game.RoundSession
import com.dolemes.braingames.ui.theme.CorrectColor
import com.dolemes.braingames.ui.theme.WrongColor
import kotlinx.coroutines.delay

/**
 * Setas no Meio (flanker; Eriksen & Eriksen, 1974). Exemplo de referência de minijogo:
 * regras em :core (FlankerRules, com testes), aqui só desenho e toques.
 */
object FlankerGame : MiniGame {
    override val definition = MiniGameDefinition(
        id = "flanker",
        nameRes = R.string.game_flanker_name,
        howToRes = R.string.game_flanker_howto,
        domain = CognitiveDomain.ATTENTION_SPEED,
        roundSeconds = 60.0,
        supportsUntimedMode = true,
        untimedTrials = 24,
        staircase = StaircaseSettings(maxLevel = FlankerRules.MAX_LEVEL),
    )

    @Composable
    override fun Play(session: RoundSession, modifier: Modifier) {
        FlankerPlay(session, modifier)
    }
}

private const val GAP_MS = 400L

@Composable
private fun FlankerPlay(session: RoundSession, modifier: Modifier) {
    var trial by remember { mutableStateOf<FlankerTrial?>(null) }
    var trialCount by remember { mutableIntStateOf(0) }
    var onsetNanos by remember { mutableLongStateOf(0L) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }

    // Ciclo de tentativas. Reinicia a cada resposta (trialCount) e a cada pausa/retomada:
    // em pausa a tentativa em curso é descartada, sem pontuar; ao retomar vem uma nova.
    LaunchedEffect(trialCount, session.isPaused, session.isRunning) {
        trial = null
        if (session.isPaused || !session.isRunning) return@LaunchedEffect
        if (trialCount > 0) delay(GAP_MS)
        feedback = null
        val next = FlankerRules.next(session.random, session.level)
        trial = next
        onsetNanos = session.nowNanos()
        onsetNanos = withFrameNanos { it } // quadro em que as setas aparecem
        delay((FlankerRules.responseWindowSeconds(session.level) * 1000).toLong())
        if (trial != null) { // não respondeu a tempo = erro
            trial = null
            feedback = false
            session.registerTrial(isCorrect = false, points = 0)
            trialCount++
        }
    }

    fun answer(right: Boolean) {
        val current = trial ?: return
        if (session.isPaused) return
        trial = null
        val reactionMs = (session.nowNanos() - onsetNanos) / 1_000_000.0
        val correct = right == current.targetRight
        feedback = correct
        session.registerTrial(correct, if (correct) session.pointsFor(10) else 0, reactionMs)
        trialCount++
    }

    val leftLabel = stringResource(R.string.flanker_left)
    val rightLabel = stringResource(R.string.flanker_right)
    Box(modifier) {
        // Duas metades da tela como alvos de toque: grandes, fáceis para qualquer idade.
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clickable(onClickLabel = leftLabel) { answer(right = false) }
                    .semantics { contentDescription = leftLabel },
            )
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clickable(onClickLabel = rightLabel) { answer(right = true) }
                    .semantics { contentDescription = rightLabel },
            )
        }
        FlankerStimulus(trial = trial, feedback = feedback, modifier = Modifier.align(Alignment.Center))
        Text("◀", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.BottomStart).padding(24.dp))
        Text("▶", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
    }
}

/** Estímulo e feedback, sem estado (usado também nas capturas de tela). */
@Composable
fun FlankerStimulus(trial: FlankerTrial?, feedback: Boolean?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = trial?.render()?.map { if (it == '<') '←' else '→' }?.joinToString(" ") ?: " ",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
        val (mark, color, label) = when (feedback) {
            true -> Triple("✓", CorrectColor, stringResource(R.string.feedback_correct))
            false -> Triple("✗", WrongColor, stringResource(R.string.feedback_wrong))
            null -> Triple(" ", MaterialTheme.colorScheme.onBackground, "")
        }
        Text(
            text = mark,
            fontSize = 48.sp,
            color = color,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
