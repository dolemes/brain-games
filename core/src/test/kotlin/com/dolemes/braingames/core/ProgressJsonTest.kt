package com.dolemes.braingames.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressJsonTest {

    @Test
    fun roundTrip_keepsEverything() {
        val result = RoundResult("flanker", 120, 20, 15, 1, 6, 7, 90.0, 480.0, true)
        val p = PlayerProgress(preferences = Preferences(untimedMode = true, textScale = 1.4f))
            .withResult(result, StaircaseState(level = 6, placementDone = true), LocalDate.of(2026, 9, 30))
        assertEquals(p, ProgressJson.decode(ProgressJson.encode(p)))
    }

    @Test
    fun oldSaveWithoutNewFields_opensWithDefaults() {
        val old = """{"schemaVersion":1,"completedSessions":4,"games":[{"gameId":"flanker"}]}"""
        val p = ProgressJson.decode(old)!!
        assertEquals(4, p.completedSessions)
        assertEquals(0, p.game("flanker")!!.staircase.level)
        assertEquals(Preferences(), p.preferences)
    }

    @Test
    fun unknownFields_areIgnored() {
        val newer = """{"schemaVersion":1,"somethingFromTheFuture":true}"""
        assertEquals(PlayerProgress(), ProgressJson.decode(newer))
    }

    @Test
    fun corruptedText_returnsNull() {
        assertNull(ProgressJson.decode("{ isto não é json"))
    }
}
