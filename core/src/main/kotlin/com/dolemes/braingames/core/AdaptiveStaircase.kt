package com.dolemes.braingames.core

import kotlinx.serialization.Serializable

/**
 * Parâmetros da escada adaptativa de um minijogo (vêm do one-pager do GDD).
 *
 * @property correctToLevelUp acertos seguidos para subir um nível; um erro desce um.
 *   2 mantém o jogador perto de 71% de acerto; 3 perto de 79% (Levitt, 1971).
 *   Prefira 3 em jogos lentos ou em que errar desanima.
 * @property warmUpDrop níveis a recuar ao retomar numa nova sessão (aquecimento).
 * @property fastStart quem nunca jogou sobe um nível a cada acerto até o primeiro erro,
 *   para um jogador experiente não passar minutos em níveis triviais.
 */
data class StaircaseSettings(
    val minLevel: Int = 1,
    val maxLevel: Int = 20,
    val startLevel: Int = 1,
    val correctToLevelUp: Int = 2,
    val step: Int = 1,
    val warmUpDrop: Int = 1,
    val fastStart: Boolean = true,
) {
    init {
        require(minLevel >= 1) { "minLevel deve ser >= 1 (0 significa 'nunca jogou')." }
        require(maxLevel >= minLevel) { "maxLevel deve ser >= minLevel." }
        require(correctToLevelUp >= 1) { "correctToLevelUp deve ser >= 1." }
        require(step >= 1) { "step deve ser >= 1." }
        require(warmUpDrop >= 0) { "warmUpDrop não pode ser negativo." }
    }
}

/** Estado da escada salvo no progresso do jogador. `level = 0` significa "nunca jogou". */
@Serializable
data class StaircaseState(
    val level: Int = 0,
    val correctStreak: Int = 0,
    val reversals: Int = 0,
    val lastDirection: Int = 0,
    val placementDone: Boolean = false,
)

enum class LevelChange { NONE, UP, DOWN }

/**
 * Escada adaptativa "N acertos sobem / 1 erro desce" (transformed up-down; Levitt, 1971).
 * Nível mais alto = tarefa mais difícil. Cada minijogo traduz o nível em parâmetros concretos
 * (tamanho da sequência, tempo de exibição, proporção de estímulos incongruentes etc.).
 */
class AdaptiveStaircase(
    private val settings: StaircaseSettings,
    saved: StaircaseState? = null,
) {
    var level: Int = settings.startLevel
        private set
    var reversals: Int = 0
        private set
    private var correctStreak = 0
    private var lastDirection = 0
    private var placementDone = !settings.fastStart

    init {
        if (saved != null && saved.level > 0) {
            level = clamp(saved.level - settings.warmUpDrop)
            reversals = saved.reversals
            placementDone = true // quem já jogou não refaz o posicionamento
        } else {
            level = clamp(settings.startLevel)
        }
    }

    val minLevel: Int get() = settings.minLevel
    val maxLevel: Int get() = settings.maxLevel

    /** Nível entre 0 (mínimo) e 1 (máximo), útil para interpolar parâmetros. */
    val normalized: Float
        get() {
            val range = settings.maxLevel - settings.minLevel
            return if (range == 0) 1f else (level - settings.minLevel).toFloat() / range
        }

    /** Registra uma tentativa e devolve a mudança de nível. */
    fun registerTrial(correct: Boolean): LevelChange {
        if (!correct) {
            correctStreak = 0
            placementDone = true
            return move(-1)
        }
        if (!placementDone) return move(+1)

        correctStreak++
        if (correctStreak < settings.correctToLevelUp) return LevelChange.NONE
        correctStreak = 0
        return move(+1)
    }

    fun snapshot(): StaircaseState =
        StaircaseState(level, correctStreak, reversals, lastDirection, placementDone)

    private fun move(direction: Int): LevelChange {
        val target = clamp(level + direction * settings.step)
        if (target == level) return LevelChange.NONE // já no limite
        if (lastDirection != 0 && lastDirection != direction) reversals++
        lastDirection = direction
        level = target
        return if (direction > 0) LevelChange.UP else LevelChange.DOWN
    }

    private fun clamp(value: Int): Int = value.coerceIn(settings.minLevel, settings.maxLevel)
}
