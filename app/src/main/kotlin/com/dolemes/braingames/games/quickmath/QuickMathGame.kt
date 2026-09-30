package com.dolemes.braingames.games.quickmath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dolemes.braingames.R
import com.dolemes.braingames.core.CognitiveDomain
import com.dolemes.braingames.core.StaircaseSettings
import com.dolemes.braingames.core.games.quickmath.MathProblem
import com.dolemes.braingames.core.games.quickmath.QuickMathRules
import com.dolemes.braingames.game.MiniGame
import com.dolemes.braingames.game.MiniGameDefinition
import com.dolemes.braingames.game.ResultMetric
import com.dolemes.braingames.game.RoundSession
import com.dolemes.braingames.ui.theme.CorrectColor
import com.dolemes.braingames.ui.theme.WrongColor
import kotlin.math.max
import kotlinx.coroutines.delay

/**
 * Cálculo Relâmpago (aritmética mental; Ashcraft, 1992). Regras, níveis e distratores em :core
 * (QuickMathRules, com testes); aqui só desenho e toques. Não há prazo por conta: só o
 * cronômetro da rodada corre.
 */
object QuickMathGame : MiniGame {
    override val definition = MiniGameDefinition(
        id = "quick-math",
        nameRes = R.string.game_quick_math_name,
        howToRes = R.string.game_quick_math_howto,
        domain = CognitiveDomain.MATH_LANGUAGE,
        roundSeconds = 90.0,
        supportsUntimedMode = true,
        untimedTrials = 20,
        staircase = StaircaseSettings(maxLevel = QuickMathRules.MAX_LEVEL),
        resultMetrics = listOf(
            ResultMetric(R.string.quick_math_per_minute) { result ->
                if (result.trials > 0) QuickMathRules.problemsPerMinute(result.trials, result.durationSeconds) else null
            },
        ),
    )

    @Composable
    override fun Play(session: RoundSession, modifier: Modifier) {
        QuickMathPlay(session, modifier)
    }
}

private const val GAP_MS = 400L
private const val REVEAL_MS = 500L

@Composable
private fun QuickMathPlay(session: RoundSession, modifier: Modifier) {
    var problem by remember { mutableStateOf<MathProblem?>(null) }
    var problemCount by remember { mutableIntStateOf(0) }
    var onsetNanos by remember { mutableLongStateOf(0L) }
    var open by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    var revealCorrect by remember { mutableStateOf(false) }

    // Ciclo de contas. Reinicia a cada resposta (problemCount) e a cada pausa/retomada: em pausa
    // a conta em curso é descartada, sem pontuar; ao retomar vem uma nova. Depois de um erro a
    // conta continua na tela por 500 ms com a resposta certa destacada.
    LaunchedEffect(problemCount, session.isPaused, session.isRunning) {
        open = false
        if (session.isPaused || !session.isRunning) {
            problem = null
            revealCorrect = false
            return@LaunchedEffect
        }
        if (problemCount > 0) {
            if (feedback == false) {
                delay(REVEAL_MS)
                revealCorrect = false
            }
            delay(GAP_MS)
        }
        feedback = null
        answered = null
        val next = QuickMathRules.next(session.random, session.level, previous = problem)
        problem = next
        open = true
        onsetNanos = session.nowNanos()
        onsetNanos = withFrameNanos { it } // quadro em que a conta aparece
    }

    fun answer(index: Int) {
        val current = problem ?: return
        if (!open || session.isPaused) return
        open = false
        val reactionMs = (session.nowNanos() - onsetNanos) / 1_000_000.0
        val correct = current.options[index] == current.answer
        answered = index
        feedback = correct
        revealCorrect = !correct
        session.registerTrial(correct, if (correct) session.pointsFor(10) else 0, reactionMs)
        problemCount++
    }

    QuickMathStimulus(
        problem = problem,
        answered = answered,
        feedback = feedback,
        revealCorrect = revealCorrect,
        onAnswer = { answer(it) },
        modifier = modifier,
    )
}

/**
 * Conta e quatro respostas, sem estado (usado também nas capturas de tela).
 *
 * @param answered índice da opção tocada, ou null enquanto não houve resposta.
 * @param feedback true se a opção tocada estava certa, false se errada.
 * @param revealCorrect depois de um erro, destaca também a resposta certa (✓).
 */
@Composable
fun QuickMathStimulus(
    problem: MathProblem?,
    answered: Int?,
    feedback: Boolean?,
    revealCorrect: Boolean,
    onAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val width = maxWidth.value
        val problemText = problem?.text ?: " "
        val problemSp = fittedSp(width - 32f, problemText.length, fontScale, maxSp = 48f, minSp = 24f)
        // Botão: (largura − margens − vão) / 2, menos o recuo interno; +2 caracteres do ✓/✗ e do espaço.
        val widest = (problem?.options?.maxOfOrNull { it.toString().length } ?: 1) + 2
        val answerSp = fittedSp((width - 48f) / 2 - 16f, widest, fontScale, maxSp = 32f, minSp = 20f)

        Column(Modifier.fillMaxSize()) {
            // A conta no alto, longe do polegar: a mão não cobre o enunciado.
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = problemText,
                    fontSize = problemSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (row in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (column in 0..1) {
                            val index = row * 2 + column
                            val mark: Boolean? = when {
                                answered == null -> null
                                index == answered -> feedback == true
                                revealCorrect && index == problem?.correctIndex -> true
                                else -> null
                            }
                            AnswerButton(
                                value = problem?.options?.get(index),
                                mark = mark,
                                fontSp = answerSp,
                                onClick = { onAnswer(index) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ✓ (verde) na certa e ✗ (vermelhão) na errada: sempre com o símbolo, nunca só a cor. */
@Composable
private fun AnswerButton(value: Int?, mark: Boolean?, fontSp: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shownMark = if (value == null) null else mark
    val correctLabel = stringResource(R.string.feedback_correct)
    val wrongLabel = stringResource(R.string.feedback_wrong)
    val colors = when (shownMark) {
        true -> ButtonDefaults.buttonColors(containerColor = CorrectColor, contentColor = Color.White)
        false -> ButtonDefaults.buttonColors(containerColor = WrongColor, contentColor = Color.White)
        null -> ButtonDefaults.buttonColors()
    }
    val prefix = when (shownMark) {
        true -> "✓ "
        false -> "✗ "
        null -> ""
    }
    Button(
        onClick = onClick,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 72.dp).semantics {
            if (value != null) {
                contentDescription = when (shownMark) {
                    true -> "$value, $correctLabel"
                    false -> "$value, $wrongLabel"
                    null -> value.toString()
                }
            }
        },
    ) {
        Text(
            text = if (value == null) " " else "$prefix$value",
            fontSize = fontSp.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Maior tamanho de fonte, entre [minSp] e [maxSp], em que [chars] caracteres cabem em [widthDp].
 * Conta 0,6 em por caractere (algarismos, sinais e espaços em negrito) e já inclui a escala de
 * fonte, para o texto grande do sistema e o do app não cortarem a conta.
 */
private fun fittedSp(widthDp: Float, chars: Int, fontScale: Float, maxSp: Float, minSp: Float): Float =
    (widthDp / (max(chars, 1) * 0.6f * fontScale)).coerceIn(minSp, maxSp)
