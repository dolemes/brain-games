package com.dolemes.braingames.core

import kotlin.math.max
import kotlin.math.roundToInt

/** Resultado de uma rodada. Produzido pelo [RoundEngine] quando a rodada termina. */
data class RoundResult(
    val gameId: String,
    val score: Int,
    val trials: Int,
    val correct: Int,
    val startLevel: Int,
    val endLevel: Int,
    val maxLevelReached: Int,
    val durationSeconds: Double,
    /** Mediana do tempo de reação em ms; null quando o jogo não mede. */
    val medianReactionMs: Double?,
    /** false quando o jogador saiu no meio da rodada. */
    val completed: Boolean,
    /** Métricas próprias do jogo (ex.: "span" no Corsi), registradas com [RoundEngine.recordMax]. */
    val metrics: Map<String, Double> = emptyMap(),
) {
    val accuracy: Double get() = if (trials == 0) 0.0 else correct.toDouble() / trials
}

/**
 * Regras de uma rodada, sem nada de Android: cronômetro (ou cota de tentativas no modo sem
 * cronômetro), pausa, escada adaptativa, pontuação e resultado. O app só chama [tick] a cada
 * quadro e [registerTrial] a cada resposta; a camada de UI (RoundSession) espelha o estado.
 */
class RoundEngine(
    val gameId: String,
    staircaseSettings: StaircaseSettings,
    savedStaircase: StaircaseState?,
    val roundSeconds: Double = 90.0,
    timed: Boolean = true,
    supportsUntimedMode: Boolean = true,
    val untimedTrials: Int = 20,
) {
    /** Jogos em que o tempo é a própria tarefa ignoram o modo sem cronômetro. */
    val isTimed: Boolean = timed || !supportsUntimedMode

    private val staircase = AdaptiveStaircase(staircaseSettings, savedStaircase)
    private val reactions = ReactionStats()
    private val startLevel = staircase.level
    private var maxLevelReached = startLevel
    private val metrics = mutableMapOf<String, Double>()

    var elapsedSeconds: Double = 0.0
        private set
    var score: Int = 0
        private set
    var trials: Int = 0
        private set
    var correct: Int = 0
        private set
    var isPaused: Boolean = false
        private set

    /** Preenchido quando a rodada termina; a partir daí nada mais muda. */
    var result: RoundResult? = null
        private set

    val isRunning: Boolean get() = result == null
    val level: Int get() = staircase.level
    val normalizedLevel: Float get() = staircase.normalized

    val timeRemainingSeconds: Double
        get() = if (isTimed) max(0.0, roundSeconds - elapsedSeconds) else 0.0

    val trialsRemaining: Int
        get() = if (isTimed) 0 else max(0, untimedTrials - trials)

    init {
        require(roundSeconds > 0) { "roundSeconds deve ser positivo." }
        require(untimedTrials > 0) { "untimedTrials deve ser positivo." }
    }

    /** Avança o relógio da rodada (não conta em pausa). Encerra quando o tempo acaba. */
    fun tick(deltaSeconds: Double) {
        if (!isRunning || isPaused || deltaSeconds <= 0) return
        elapsedSeconds += deltaSeconds
        if (isTimed && elapsedSeconds >= roundSeconds) finish(completed = true)
    }

    fun setPaused(paused: Boolean) {
        if (isRunning) isPaused = paused
    }

    /**
     * Registra uma tentativa. Pode encerrar a rodada (cota do modo sem cronômetro):
     * confira [isRunning] antes de apresentar a próxima.
     *
     * @param points pontos se correto (use [pointsFor] para escalar com o nível).
     * @param reactionMs tempo de reação em ms, ou null se o jogo não mede.
     */
    fun registerTrial(isCorrect: Boolean, points: Int, reactionMs: Double? = null): LevelChange {
        if (!isRunning || isPaused) return LevelChange.NONE
        trials++
        if (isCorrect) {
            correct++
            score += max(0, points)
            if (reactionMs != null) reactions.add(reactionMs)
        }
        val change = staircase.registerTrial(isCorrect)
        maxLevelReached = max(maxLevelReached, staircase.level)
        if (!isTimed && trials >= untimedTrials) finish(completed = true)
        return change
    }

    /**
     * Guarda o maior valor visto de uma métrica do jogo (ex.: a maior sequência acertada).
     * Vai para [RoundResult.metrics]. Depois do fim da rodada, nada mais muda.
     */
    fun recordMax(key: String, value: Double) {
        if (!isRunning) return
        metrics[key] = maxOf(metrics[key] ?: value, value)
    }

    /** Pontos-base escalados pelo nível atual (+10% por nível acima do mínimo). */
    fun pointsFor(basePoints: Int): Int =
        (basePoints * (1.0 + 0.1 * (staircase.level - staircase.minLevel))).roundToInt()

    /** Encerra a rodada (idempotente). completed = false quando o jogador sai no meio. */
    fun finish(completed: Boolean): RoundResult {
        result?.let { return it }
        isPaused = false
        val r = RoundResult(
            gameId = gameId,
            score = score,
            trials = trials,
            correct = correct,
            startLevel = startLevel,
            endLevel = staircase.level,
            maxLevelReached = maxLevelReached,
            durationSeconds = elapsedSeconds,
            medianReactionMs = reactions.median(),
            completed = completed,
            metrics = metrics.toMap(),
        )
        result = r
        return r
    }

    /** Estado da escada para salvar no progresso. */
    fun finalStaircase(): StaircaseState = staircase.snapshot()
}
