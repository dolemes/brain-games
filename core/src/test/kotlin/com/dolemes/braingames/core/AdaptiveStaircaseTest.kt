package com.dolemes.braingames.core

import kotlin.math.exp
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveStaircaseTest {

    private fun settings(fastStart: Boolean = false) = StaircaseSettings(
        minLevel = 1, maxLevel = 10, startLevel = 1,
        correctToLevelUp = 2, step = 1, warmUpDrop = 1, fastStart = fastStart,
    )

    @Test
    fun newPlayer_startsAtStartLevel() {
        assertEquals(1, AdaptiveStaircase(settings()).level)
    }

    @Test
    fun twoCorrect_raiseOneLevel() {
        val s = AdaptiveStaircase(settings())
        assertEquals(LevelChange.NONE, s.registerTrial(true))
        assertEquals(LevelChange.UP, s.registerTrial(true))
        assertEquals(2, s.level)
    }

    @Test
    fun error_lowersLevelAndResetsStreak() {
        val s = AdaptiveStaircase(settings(), StaircaseState(level = 6))
        assertEquals(5, s.level) // aquecimento: recua 1
        s.registerTrial(true)
        assertEquals(LevelChange.DOWN, s.registerTrial(false))
        assertEquals(4, s.level)
        assertEquals(LevelChange.NONE, s.registerTrial(true)) // sequência zerada pelo erro
    }

    @Test
    fun level_isClampedAtBounds() {
        val bottom = AdaptiveStaircase(settings())
        assertEquals(LevelChange.NONE, bottom.registerTrial(false))
        assertEquals(1, bottom.level)

        val top = AdaptiveStaircase(settings(), StaircaseState(level = 11))
        assertEquals(10, top.level)
        top.registerTrial(true)
        assertEquals(LevelChange.NONE, top.registerTrial(true))
        assertEquals(10, top.level)
    }

    @Test
    fun fastStart_raisesEveryCorrectUntilFirstError() {
        val s = AdaptiveStaircase(settings(fastStart = true))
        repeat(3) { s.registerTrial(true) }
        assertEquals(4, s.level)
        s.registerTrial(false)
        assertEquals(3, s.level)
        assertEquals(LevelChange.NONE, s.registerTrial(true)) // agora exige 2 acertos
    }

    @Test
    fun fastStart_isSkippedForReturningPlayers() {
        val s = AdaptiveStaircase(settings(fastStart = true), StaircaseState(level = 5))
        assertEquals(LevelChange.NONE, s.registerTrial(true))
    }

    @Test
    fun reversals_areCounted() {
        val s = AdaptiveStaircase(settings(), StaircaseState(level = 5))
        s.registerTrial(true); s.registerTrial(true) // sobe
        s.registerTrial(false)                       // desce: 1ª reversão
        s.registerTrial(true); s.registerTrial(true) // sobe: 2ª reversão
        assertEquals(2, s.reversals)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSettings_throw() {
        StaircaseSettings(minLevel = 0)
    }

    @Test
    fun snapshot_roundTripsLevel() {
        val s = AdaptiveStaircase(settings(), StaircaseState(level = 7))
        val resumed = AdaptiveStaircase(settings(), s.snapshot())
        assertEquals(5, resumed.level) // 7 → aquecimento 6 → novo aquecimento 5
    }

    /**
     * Jogador simulado cuja chance de acerto cai com o nível. Com a regra 2/1 a escada
     * deve estabilizar perto de 70,7% de acerto (Levitt, 1971).
     */
    @Test
    fun twoUpOneDown_convergesNear71Percent() {
        val s = AdaptiveStaircase(StaircaseSettings(maxLevel = 40, fastStart = false))
        val rng = Random(12345)
        val trials = 20_000
        var counted = 0
        var correct = 0
        for (i in 0 until trials) {
            val pCorrect = 1.0 / (1.0 + exp((s.level - 20) / 3.0))
            val ok = rng.nextDouble() < pCorrect
            if (i >= trials / 2) { counted++; if (ok) correct++ }
            s.registerTrial(ok)
        }
        val accuracy = correct.toDouble() / counted
        assertTrue("acerto = $accuracy", accuracy in 0.66..0.76)
    }
}
