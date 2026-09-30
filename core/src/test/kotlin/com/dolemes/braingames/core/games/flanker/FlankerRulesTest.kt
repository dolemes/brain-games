package com.dolemes.braingames.core.games.flanker

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlankerRulesTest {

    @Test
    fun responseWindow_shrinksWithLevel() {
        assertEquals(2.5, FlankerRules.responseWindowSeconds(1), 1e-9)
        assertEquals(0.9, FlankerRules.responseWindowSeconds(20), 1e-9)
        assertTrue(FlankerRules.responseWindowSeconds(10) < FlankerRules.responseWindowSeconds(9))
    }

    @Test
    fun incongruentRatio_matchesTableAtLevel1() {
        val rng = Random(7)
        val n = 10_000
        val incongruent = (1..n).count { !FlankerRules.next(rng, 1).congruent }
        assertEquals(0.25, incongruent.toDouble() / n, 0.02)
    }

    @Test
    fun render_centerDiffersFromSidesWhenIncongruent() {
        val s = FlankerTrial(targetRight = true, congruent = false).render()
        assertEquals("<<><<", s)
        assertNotEquals(s[0], s[2])
        assertEquals(">>>>>", FlankerTrial(targetRight = true, congruent = true).render())
    }
}
