package com.dolemes.braingames.core

/**
 * Tempos de reação da rodada. Usa a mediana porque tempo de reação tem cauda longa
 * (distração, toque perdido): uma resposta de 4 s não deve distorcer o resultado.
 */
class ReactionStats {
    private val samples = mutableListOf<Double>()

    val count: Int get() = samples.size

    fun clear() = samples.clear()

    /** Registra um tempo em ms; devolve false (e ignora) se for rápido demais para ser reação. */
    fun add(reactionMs: Double): Boolean {
        if (reactionMs < MIN_PLAUSIBLE_MS) return false
        samples += reactionMs
        return true
    }

    /** Mediana em ms, ou null sem amostras. */
    fun median(): Double? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    companion object {
        /** Respostas mais rápidas que isso são antecipação, não reação. */
        const val MIN_PLAUSIBLE_MS = 150.0
    }
}
