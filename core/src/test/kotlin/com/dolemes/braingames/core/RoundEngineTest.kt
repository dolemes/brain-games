package com.dolemes.braingames.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundEngineTest {

    private fun engine(timed: Boolean = true, supportsUntimed: Boolean = true) = RoundEngine(
        gameId = "flanker",
        staircaseSettings = StaircaseSettings(fastStart = false),
        savedStaircase = null,
        roundSeconds = 10.0,
        timed = timed,
        supportsUntimedMode = supportsUntimed,
        untimedTrials = 3,
    )

    @Test
    fun timedRound_endsWhenTimeRunsOut() {
        val e = engine()
        e.tick(9.5)
        assertTrue(e.isRunning)
        e.tick(0.6)
        assertFalse(e.isRunning)
        assertTrue(e.result!!.completed)
        assertEquals(0.0, e.timeRemainingSeconds, 1e-9)
    }

    @Test
    fun pause_freezesClockAndIgnoresAnswers() {
        val e = engine()
        e.setPaused(true)
        e.tick(60.0)
        assertEquals(LevelChange.NONE, e.registerTrial(true, 10))
        assertEquals(0, e.trials)
        assertTrue(e.isRunning)
        e.setPaused(false)
        e.tick(1.0)
        assertEquals(1.0, e.elapsedSeconds, 1e-9)
    }

    @Test
    fun untimedRound_endsAfterQuota() {
        val e = engine(timed = false)
        assertFalse(e.isTimed)
        e.tick(1000.0) // o relógio não encerra no modo sem cronômetro
        assertTrue(e.isRunning)
        repeat(3) { e.registerTrial(true, 10) }
        assertFalse(e.isRunning)
        assertEquals(30, e.result!!.score)
    }

    @Test
    fun gamesWhereTimeIsTheTask_ignoreUntimedMode() {
        assertTrue(engine(timed = false, supportsUntimed = false).isTimed)
    }

    @Test
    fun scoring_countsOnlyCorrectAndMedianReaction() {
        val e = engine()
        e.registerTrial(true, 10, reactionMs = 400.0)
        e.registerTrial(false, 10, reactionMs = 300.0) // erro: sem pontos, sem tempo
        e.registerTrial(true, 10, reactionMs = 600.0)
        val r = e.finish(completed = true)
        assertEquals(20, r.score)
        assertEquals(3, r.trials)
        assertEquals(2, r.correct)
        assertEquals(500.0, r.medianReactionMs!!, 1e-9)
    }

    @Test
    fun finish_isIdempotentAndFreezesState() {
        val e = engine()
        val first = e.finish(completed = false)
        val second = e.finish(completed = true)
        assertSame(first, second)
        assertFalse(first.completed)
        assertEquals(LevelChange.NONE, e.registerTrial(true, 10))
        assertEquals(0, e.trials)
    }

    @Test
    fun pointsFor_scalesWithLevel() {
        val e = RoundEngine("x", StaircaseSettings(fastStart = false), StaircaseState(level = 6), roundSeconds = 10.0)
        assertEquals(5, e.level) // aquecimento
        assertEquals(14, e.pointsFor(10)) // +10% por nível acima do mínimo
    }

    @Test
    fun noReactionTimes_meansNullMedian() {
        val e = engine()
        e.registerTrial(true, 10)
        assertNull(e.finish(true).medianReactionMs)
        assertNotNull(e.finalStaircase())
    }
}
