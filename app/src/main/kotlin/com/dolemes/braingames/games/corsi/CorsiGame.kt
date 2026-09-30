package com.dolemes.braingames.games.corsi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dolemes.braingames.R
import com.dolemes.braingames.core.CognitiveDomain
import com.dolemes.braingames.core.StaircaseSettings
import com.dolemes.braingames.core.games.corsi.BlockCenter
import com.dolemes.braingames.core.games.corsi.CorsiAttempt
import com.dolemes.braingames.core.games.corsi.CorsiRules
import com.dolemes.braingames.core.games.corsi.TapResult
import com.dolemes.braingames.game.MiniGame
import com.dolemes.braingames.game.MiniGameDefinition
import com.dolemes.braingames.game.ResultMetric
import com.dolemes.braingames.game.RoundSession
import com.dolemes.braingames.ui.theme.CorrectColor
import com.dolemes.braingames.ui.theme.WrongColor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Chave da métrica "maior sequência acertada" no resultado da rodada. */
private const val SPAN_KEY = "span"

/**
 * Sequência de Luzes (blocos de Corsi; Corsi, 1972). Posições, sequência e conferência da
 * resposta ficam em :core (CorsiRules e CorsiAttempt, com testes); aqui só desenho e toques.
 * Não há prazo por sequência: só o cronômetro da rodada corre.
 */
object CorsiGame : MiniGame {
    override val definition = MiniGameDefinition(
        id = "corsi",
        nameRes = R.string.game_corsi_name,
        howToRes = R.string.game_corsi_howto,
        domain = CognitiveDomain.MEMORY,
        roundSeconds = 90.0,
        supportsUntimedMode = true,
        untimedTrials = 10,
        staircase = StaircaseSettings(maxLevel = CorsiRules.MAX_LEVEL),
        resultMetrics = listOf(
            ResultMetric(R.string.corsi_span) { result -> result.metrics[SPAN_KEY]?.roundToInt() },
        ),
    )

    @Composable
    override fun Play(session: RoundSession, modifier: Modifier) {
        CorsiPlay(session, modifier)
    }
}

private const val LEAD_IN_MS = 400L
private const val GAP_MS = 600L
private const val REVEAL_MS = 500L
private const val TAP_FLASH_MS = 150L

@Composable
private fun CorsiPlay(session: RoundSession, modifier: Modifier) {
    var sequence by remember { mutableStateOf<List<Int>>(emptyList()) }
    var attempt by remember { mutableStateOf<CorsiAttempt?>(null) }
    var lit by remember { mutableStateOf<Int?>(null) }
    var tapped by remember { mutableStateOf<Int?>(null) }
    var tapCount by remember { mutableIntStateOf(0) }
    var sequenceCount by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    var revealOrder by remember { mutableStateOf(false) }
    var lastTapBlock by remember { mutableIntStateOf(-1) }
    var lastTapNanos by remember { mutableLongStateOf(0L) }

    Column(modifier.fillMaxSize()) {
        CorsiStatus(yourTurn = attempt != null, feedback = feedback)
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            val width = maxWidth.value
            val height = maxHeight.value
            if (width <= 0f || height <= 0f) return@BoxWithConstraints
            val blockDp = remember(width, height) { CorsiRules.blockSizeFor(width, height) }
            // Posições novas a cada rodada (e se o campo mudar de tamanho): nunca em grade.
            val positions = remember(width, height) { CorsiRules.layout(session.random, width, height, blockDp) }

            // Ciclo de sequências. Reinicia a cada resposta (sequenceCount) e a cada pausa/retomada:
            // em pausa a sequência em curso é descartada, sem pontuar; ao retomar vem outra. Durante
            // a exibição (attempt == null) os toques são ignorados.
            LaunchedEffect(positions, sequenceCount, session.isPaused, session.isRunning) {
                attempt = null
                lit = null
                tapped = null
                if (session.isPaused || !session.isRunning) {
                    revealOrder = false
                    return@LaunchedEffect
                }
                if (sequenceCount > 0) {
                    if (revealOrder) { // depois de um erro, mostra a ordem certa por 500 ms
                        delay(REVEAL_MS)
                        revealOrder = false
                    }
                    delay(GAP_MS)
                }
                feedback = null
                val next = CorsiRules.sequence(
                    session.random,
                    positions,
                    CorsiRules.sequenceLength(session.level),
                    blockDp,
                )
                sequence = next
                delay(LEAD_IN_MS)
                for (block in next) {
                    lit = block
                    delay(CorsiRules.litMillis(session.level))
                    lit = null
                    delay(CorsiRules.GAP_BETWEEN_BLOCKS_MS)
                }
                attempt = CorsiAttempt(next)
            }
            // O bloco tocado fica marcado por um instante.
            LaunchedEffect(tapCount) {
                if (tapCount > 0) {
                    delay(TAP_FLASH_MS)
                    tapped = null
                }
            }

            fun onTap(index: Int) {
                val current = attempt ?: return
                if (session.isPaused) return
                val now = session.nowNanos()
                // Dois toques no mesmo bloco em menos de 200 ms contam como um só.
                if (index == lastTapBlock && now - lastTapNanos < CorsiRules.DOUBLE_TAP_MS * 1_000_000L) return
                lastTapBlock = index
                lastTapNanos = now
                tapped = index
                tapCount++
                when (current.tap(index)) {
                    TapResult.CORRECT, TapResult.IGNORED -> Unit
                    TapResult.COMPLETE -> {
                        attempt = null
                        feedback = true
                        // Métrica antes do registro: registerTrial pode encerrar a rodada.
                        session.recordMax(SPAN_KEY, current.sequence.size.toDouble())
                        session.registerTrial(
                            isCorrect = true,
                            points = session.pointsFor(CorsiRules.basePoints(current.sequence.size)),
                        )
                        sequenceCount++
                    }
                    TapResult.WRONG -> {
                        attempt = null
                        feedback = false
                        revealOrder = true
                        session.registerTrial(isCorrect = false, points = 0)
                        sequenceCount++
                    }
                }
            }

            CorsiField(
                positions = positions,
                blockDp = blockDp,
                lit = lit,
                tapped = tapped,
                order = if (revealOrder) sequence else null,
                onTap = { onTap(it) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** "Observe…" durante a exibição, "Sua vez!" na resposta, e ✓/✗ ao fim da tentativa. */
@Composable
fun CorsiStatus(yourTurn: Boolean, feedback: Boolean?, modifier: Modifier = Modifier) {
    val correctLabel = stringResource(R.string.feedback_correct)
    val wrongLabel = stringResource(R.string.feedback_wrong)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(if (yourTurn) R.string.corsi_your_turn else R.string.corsi_watch),
            style = MaterialTheme.typography.titleLarge,
        )
        when (feedback) {
            true -> Text(
                text = "  ✓",
                fontSize = 32.sp,
                color = CorrectColor,
                modifier = Modifier.semantics { contentDescription = correctLabel },
            )
            false -> Text(
                text = "  ✗",
                fontSize = 32.sp,
                color = WrongColor,
                modifier = Modifier.semantics { contentDescription = wrongLabel },
            )
            null -> Unit
        }
    }
}

/**
 * Os 9 blocos nas [positions] (centros, em dp), sem estado (usado também nas capturas de tela).
 *
 * @param lit bloco aceso agora (cor, brilho e borda de 4 dp: nunca só a cor).
 * @param tapped bloco tocado agora, marcado por um instante.
 * @param order depois de um erro, a ordem certa dos blocos: cada um mostra seu número (1, 2, 3…).
 */
@Composable
fun CorsiField(
    positions: List<BlockCenter>,
    blockDp: Float,
    lit: Int?,
    tapped: Int?,
    order: List<Int>?,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val litLabel = stringResource(R.string.corsi_block_lit)
    Box(modifier) {
        positions.forEachIndexed { index, center ->
            val position = order?.indexOf(index) ?: -1
            CorsiBlock(
                label = stringResource(R.string.corsi_block, index + 1),
                litLabel = litLabel,
                lit = index == lit,
                pressed = index == tapped,
                number = if (position >= 0) position + 1 else null,
                onClick = { onTap(index) },
                modifier = Modifier
                    .offset(x = (center.x - blockDp / 2).dp, y = (center.y - blockDp / 2).dp)
                    .size(blockDp.dp),
            )
        }
    }
}

@Composable
private fun CorsiBlock(
    label: String,
    litLabel: String,
    lit: Boolean,
    pressed: Boolean,
    number: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    val fill = when {
        lit -> colors.primary
        pressed -> colors.primary.copy(alpha = 0.35f)
        else -> colors.surfaceVariant
    }
    val border = if (lit) {
        BorderStroke(4.dp, colors.onBackground)
    } else {
        BorderStroke(2.dp, colors.onSurfaceVariant.copy(alpha = 0.6f))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(border, shape)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                if (lit) stateDescription = litLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
        }
    }
}
