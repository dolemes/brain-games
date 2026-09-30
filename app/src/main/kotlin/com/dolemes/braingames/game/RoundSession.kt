package com.dolemes.braingames.game

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dolemes.braingames.core.LevelChange
import com.dolemes.braingames.core.RoundEngine
import com.dolemes.braingames.core.RoundResult
import com.dolemes.braingames.core.StaircaseState
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Ponte entre o [RoundEngine] (regras puras, testadas no :core) e o Compose: espelha o estado
 * do motor em estados observáveis, para a tela recompor quando algo muda.
 *
 * @property random aleatoriedade da rodada. Use sempre esta, nunca Random.Default.
 */
@Stable
class RoundSession(
    private val engine: RoundEngine,
    val random: Random,
) {
    var level by mutableIntStateOf(engine.level)
        private set
    var score by mutableIntStateOf(engine.score)
        private set
    var trials by mutableIntStateOf(engine.trials)
        private set
    var isPaused by mutableStateOf(engine.isPaused)
        private set
    /** Segundos restantes (arredondados para cima): muda uma vez por segundo, não a cada quadro. */
    var secondsLeft by mutableIntStateOf(ceil(engine.timeRemainingSeconds).toInt())
        private set
    var trialsRemaining by mutableIntStateOf(engine.trialsRemaining)
        private set
    var result by mutableStateOf<RoundResult?>(engine.result)
        private set

    val isRunning: Boolean get() = result == null
    val isTimed: Boolean get() = engine.isTimed

    /** Nível entre 0 e 1, para interpolar parâmetros. */
    val normalizedLevel: Float get() = engine.normalizedLevel

    /** Registra uma resposta. Pode encerrar a rodada: confira [isRunning] antes da próxima tentativa. */
    fun registerTrial(isCorrect: Boolean, points: Int, reactionMs: Double? = null): LevelChange =
        engine.registerTrial(isCorrect, points, reactionMs).also { sync() }

    /** Pontos-base escalados pelo nível (+10% por nível). */
    fun pointsFor(basePoints: Int): Int = engine.pointsFor(basePoints)

    /** Relógio para tempo de reação, em nanossegundos. É o mesmo relógio de withFrameNanos. */
    fun nowNanos(): Long = System.nanoTime()

    // ---- Usado pelo GameHostScreen ----

    fun tick(deltaSeconds: Double) {
        engine.tick(deltaSeconds)
        sync()
    }

    fun setPaused(paused: Boolean) {
        engine.setPaused(paused)
        sync()
    }

    /** O jogador saiu no meio: a rodada não conta, mas o nível é preservado. */
    fun abort() {
        engine.finish(completed = false)
        sync()
    }

    fun finalStaircase(): StaircaseState = engine.finalStaircase()

    private fun sync() {
        level = engine.level
        score = engine.score
        trials = engine.trials
        isPaused = engine.isPaused
        secondsLeft = ceil(engine.timeRemainingSeconds).toInt()
        trialsRemaining = engine.trialsRemaining
        result = engine.result
    }
}
