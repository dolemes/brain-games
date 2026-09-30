package com.dolemes.braingames.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private fun round(id: String, score: Int = 10, completed: Boolean = true) = RoundResult(
    gameId = id, score = score, trials = 10, correct = 7, startLevel = 1, endLevel = 3,
    maxLevelReached = 3, durationSeconds = 90.0, medianReactionMs = null, completed = completed,
)

class PlayerProgressTest {

    @Test
    fun streak_growsOnConsecutiveDays_andResetsAfterGap() {
        var p = PlayerProgress()
        p = p.withResult(round("a"), null, LocalDate.of(2026, 9, 1))
        p = p.withResult(round("a"), null, LocalDate.of(2026, 9, 1)) // mesmo dia
        p = p.withResult(round("a"), null, LocalDate.of(2026, 9, 2))
        assertEquals(2, p.currentStreakDays)
        p = p.withResult(round("a"), null, LocalDate.of(2026, 9, 5))
        assertEquals(1, p.currentStreakDays)
        assertEquals(2, p.bestStreakDays)
    }

    @Test
    fun abandonedRound_keepsLevelButDoesNotCount() {
        val p = PlayerProgress().withResult(round("a", score = 999, completed = false), StaircaseState(level = 7), LocalDate.of(2026, 9, 1))
        val g = p.game("a")!!
        assertEquals(7, g.staircase.level)
        assertEquals(0, g.timesPlayed)
        assertEquals(0, g.bestScore)
        assertEquals(0, p.completedSessions)
        assertEquals("", p.lastPlayedDate)
    }

    @Test
    fun recentHistory_isCapped() {
        var p = PlayerProgress()
        repeat(PlayerProgress.MAX_RECENT_PER_GAME + 5) { i ->
            p = p.withResult(round("a", score = i), null, LocalDate.of(2026, 9, 1))
        }
        assertEquals(PlayerProgress.MAX_RECENT_PER_GAME, p.game("a")!!.recent.size)
        assertEquals(PlayerProgress.MAX_RECENT_PER_GAME + 4, p.game("a")!!.bestScore)
    }

    @Test
    fun original_isNotMutated() {
        val before = PlayerProgress()
        before.withResult(round("a"), null, LocalDate.of(2026, 9, 1))
        assertEquals(PlayerProgress(), before)
    }
}

class DailyWorkoutPlannerTest {

    private val catalog = listOf(
        CatalogEntry("corsi", CognitiveDomain.MEMORY),
        CatalogEntry("pairs", CognitiveDomain.MEMORY),
        CatalogEntry("matrices", CognitiveDomain.REASONING),
        CatalogEntry("stroop", CognitiveDomain.ATTENTION_SPEED),
        CatalogEntry("flanker", CognitiveDomain.ATTENTION_SPEED),
        CatalogEntry("mental-math", CognitiveDomain.MATH_LANGUAGE),
    )

    @Test
    fun plan_usesDistinctDomains() {
        val p = DailyWorkoutPlanner.ensureToday(PlayerProgress(), catalog, LocalDate.of(2026, 9, 30))
        assertEquals(3, p.workout.gameIds.size)
        val domains = p.workout.gameIds.map { id -> catalog.first { it.id == id }.domain }.toSet()
        assertEquals(3, domains.size)
    }

    @Test
    fun plan_isStableDuringTheDay_evenAfterPlaying() {
        val day = LocalDate.of(2026, 9, 30)
        var p = DailyWorkoutPlanner.ensureToday(PlayerProgress(), catalog, day)
        val first = p.workout.gameIds
        p = p.withResult(round(first[0]), null, day)
        val again = DailyWorkoutPlanner.ensureToday(p, catalog, day)
        assertSame(p, again)
        assertEquals(first, again.workout.gameIds)
        assertEquals(listOf(first[0]), again.workout.doneIds)
    }

    @Test
    fun newDay_resetsDoneList() {
        val day = LocalDate.of(2026, 9, 30)
        var p = DailyWorkoutPlanner.ensureToday(PlayerProgress(), catalog, day)
        for (id in p.workout.gameIds) p = p.withResult(round(id), null, day)
        assertTrue(p.workout.isComplete(day))

        p = DailyWorkoutPlanner.ensureToday(p, catalog, day.plusDays(1))
        assertTrue(p.workout.doneIds.isEmpty())
        assertFalse(p.workout.isComplete(day.plusDays(1)))
    }

    @Test
    fun plan_fillsWhenThereAreFewerDomainsThanGames() {
        val small = listOf(
            CatalogEntry("a", CognitiveDomain.MEMORY),
            CatalogEntry("b", CognitiveDomain.MEMORY),
            CatalogEntry("c", CognitiveDomain.REASONING),
        )
        val plan = DailyWorkoutPlanner.plan(small, PlayerProgress(), LocalDate.of(2026, 9, 30), 3)
        assertEquals(3, plan.size)
        assertEquals(3, plan.toSet().size)
    }

    @Test
    fun plan_withSingleGame_hasOneGame() {
        val plan = DailyWorkoutPlanner.plan(listOf(CatalogEntry("flanker", CognitiveDomain.ATTENTION_SPEED)), PlayerProgress(), LocalDate.of(2026, 9, 30), 3)
        assertEquals(listOf("flanker"), plan)
    }

    @Test
    fun plan_prefersLeastPlayed() {
        val day = LocalDate.of(2026, 9, 30)
        var p = PlayerProgress()
        repeat(5) { p = p.withResult(round("corsi"), null, day) }
        val plan = DailyWorkoutPlanner.plan(catalog, p, day, 4)
        assertTrue("pairs" in plan)
        assertFalse("corsi" in plan)
    }
}

class AdPolicyTest {

    @Test
    fun noAd_forNewPlayers() {
        val policy = AdPolicy()
        repeat(5) { policy.onRoundFinished() }
        assertFalse(policy.shouldShowInterstitial(completedSessions = 2, nowSeconds = 1000.0))
    }

    @Test
    fun noAd_beforeEnoughRounds() {
        val policy = AdPolicy()
        policy.onRoundFinished()
        assertFalse(policy.shouldShowInterstitial(10, 1000.0))
        policy.onRoundFinished()
        assertTrue(policy.shouldShowInterstitial(10, 1000.0))
    }

    @Test
    fun noAd_duringCooldown() {
        val policy = AdPolicy()
        policy.onInterstitialShown(1000.0)
        repeat(2) { policy.onRoundFinished() }
        assertFalse(policy.shouldShowInterstitial(10, 1100.0))
        assertTrue(policy.shouldShowInterstitial(10, 1181.0))
    }
}

class ReactionStatsTest {

    @Test
    fun median_ignoresAnticipations() {
        val r = ReactionStats()
        assertFalse(r.add(90.0))
        r.add(400.0); r.add(500.0); r.add(4000.0)
        assertEquals(500.0, r.median()!!, 1e-9)
        r.add(450.0)
        assertEquals(475.0, r.median()!!, 1e-9)
    }

    @Test
    fun median_isNullWithoutSamples() {
        assertNull(ReactionStats().median())
    }
}
