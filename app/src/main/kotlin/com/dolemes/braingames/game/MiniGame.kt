package com.dolemes.braingames.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dolemes.braingames.R
import com.dolemes.braingames.core.CognitiveDomain
import com.dolemes.braingames.core.StaircaseSettings

/**
 * Ficha de um minijogo. Os valores vêm do one-pager do GDD.
 *
 * @property id identificador estável em kebab-case (ex.: "flanker"). É a chave do save:
 *   nunca mude depois de publicar.
 * @property supportsUntimedMode false quando o tempo é a própria tarefa (n-back, Go/No-Go).
 * @property untimedTrials no modo sem cronômetro, a rodada termina após este número de respostas.
 */
data class MiniGameDefinition(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val howToRes: Int,
    val domain: CognitiveDomain,
    val roundSeconds: Double = 90.0,
    val supportsUntimedMode: Boolean = true,
    val untimedTrials: Int = 20,
    val staircase: StaircaseSettings = StaircaseSettings(),
)

/**
 * Contrato de todo minijogo. O minijogo só desenha a rodada e repassa as respostas para a
 * [RoundSession]; instrução, contagem, cronômetro, pausa, resultado, save e anúncios ficam
 * no GameHostScreen. Regras e geradores de estímulo ficam no módulo :core, com testes.
 */
interface MiniGame {
    val definition: MiniGameDefinition

    /**
     * Desenha a rodada. Leia [RoundSession.level] para montar cada tentativa, chame
     * [RoundSession.registerTrial] a cada resposta, e observe [RoundSession.isPaused] e
     * [RoundSession.isRunning]: em pausa, esconda o estímulo e descarte a tentativa em curso.
     */
    @Composable
    fun Play(session: RoundSession, modifier: Modifier)
}

@StringRes
fun CognitiveDomain.labelRes(): Int = when (this) {
    CognitiveDomain.MEMORY -> R.string.domain_memory
    CognitiveDomain.REASONING -> R.string.domain_reasoning
    CognitiveDomain.ATTENTION_SPEED -> R.string.domain_attention
    CognitiveDomain.MATH_LANGUAGE -> R.string.domain_math_language
}
