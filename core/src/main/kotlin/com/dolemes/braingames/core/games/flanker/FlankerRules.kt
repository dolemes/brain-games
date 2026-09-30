package com.dolemes.braingames.core.games.flanker

import kotlin.random.Random

/** Uma tentativa do flanker: direção da seta central e se as laterais concordam com ela. */
data class FlankerTrial(val targetRight: Boolean, val congruent: Boolean) {
    /** Texto do estímulo, ex.: "<<><<" (incongruente, alvo à direita). */
    fun render(): String {
        val center = if (targetRight) '>' else '<'
        val side = if (congruent) center else if (targetRight) '<' else '>'
        return charArrayOf(side, side, center, side, side).concatToString()
    }
}

/**
 * Regras puras do flanker (Eriksen & Eriksen, 1974): nível → parâmetros e sorteio.
 * Nível 1: 25% de incongruentes e 2,5 s para responder; nível 20: 70% e 0,9 s.
 */
object FlankerRules {
    const val MAX_LEVEL = 20

    fun incongruentRatio(level: Int): Double = lerp(0.25, 0.70, level)

    fun responseWindowSeconds(level: Int): Double = lerp(2.5, 0.9, level)

    fun next(random: Random, level: Int): FlankerTrial =
        FlankerTrial(
            targetRight = random.nextBoolean(),
            congruent = random.nextDouble() >= incongruentRatio(level),
        )

    private fun lerp(atLevel1: Double, atMax: Double, level: Int): Double {
        val t = ((level - 1).toDouble() / (MAX_LEVEL - 1)).coerceIn(0.0, 1.0)
        return atLevel1 + (atMax - atLevel1) * t
    }
}
