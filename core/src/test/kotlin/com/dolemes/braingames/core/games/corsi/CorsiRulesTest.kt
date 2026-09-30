package com.dolemes.braingames.core.games.corsi

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CorsiRulesTest {

    private val block = 64f
    private val fields = listOf(360f to 480f, 320f to 400f, 411f to 700f, 800f to 500f)

    private fun layouts(width: Float, height: Float, seeds: IntRange = 0..299) =
        seeds.map { CorsiRules.layout(Random(it), width, height, block) }

    private fun chebyshev(a: BlockCenter, b: BlockCenter) = max(abs(a.x - b.x), abs(a.y - b.y))

    // ---- Tabela de níveis (GDD, seção 14) ----

    @Test
    fun sequenceLength_followsTheLevelTable() {
        assertEquals(3, CorsiRules.sequenceLength(1))
        assertEquals(3, CorsiRules.sequenceLength(2))
        assertEquals(4, CorsiRules.sequenceLength(4))
        assertEquals(6, CorsiRules.sequenceLength(7))
        assertEquals(7, CorsiRules.sequenceLength(10))
        assertEquals(9, CorsiRules.sequenceLength(13))
        assertEquals(9, CorsiRules.sequenceLength(14))
    }

    @Test
    fun sequenceLength_neverShrinksAndNeverExceedsTheBlocks() {
        val lengths = (1..CorsiRules.MAX_LEVEL).map { CorsiRules.sequenceLength(it) }
        assertEquals(lengths.sorted(), lengths)
        assertTrue(lengths.all { it in 3..CorsiRules.BLOCK_COUNT })
        assertEquals(3, CorsiRules.sequenceLength(-5))
        assertEquals(9, CorsiRules.sequenceLength(99))
    }

    @Test
    fun litMillis_followsTheLevelTable() {
        assertEquals(780L, CorsiRules.litMillis(1))
        assertEquals(720L, CorsiRules.litMillis(4))
        assertEquals(660L, CorsiRules.litMillis(7))
        assertEquals(600L, CorsiRules.litMillis(10))
        assertEquals(520L, CorsiRules.litMillis(14))
    }

    @Test
    fun litMillis_shrinksWithLevelButNeverBelow450() {
        val times = (1..CorsiRules.MAX_LEVEL).map { CorsiRules.litMillis(it) }
        assertEquals(times.sortedDescending(), times)
        assertTrue(times.all { it >= 450L })
        assertEquals(780L, CorsiRules.litMillis(-3))
        assertEquals(520L, CorsiRules.litMillis(99))
    }

    @Test
    fun basePoints_are10PerBlock() {
        assertEquals(30, CorsiRules.basePoints(3))
        assertEquals(90, CorsiRules.basePoints(9))
    }

    // ---- Posições dos blocos ----

    @Test
    fun layout_hasNineBlocksInsideTheFieldAndNoOverlap() {
        for ((w, h) in fields) {
            layouts(w, h).forEach { l ->
                assertEquals(CorsiRules.BLOCK_COUNT, l.size)
                l.forEach {
                    assertTrue("fora do campo: $it em $w × $h", it.x >= block / 2 && it.x <= w - block / 2)
                    assertTrue("fora do campo: $it em $w × $h", it.y >= block / 2 && it.y <= h - block / 2)
                }
                for (i in l.indices) for (j in i + 1 until l.size) {
                    assertTrue("blocos se tocam em $w × $h: ${l[i]} e ${l[j]}", chebyshev(l[i], l[j]) >= block)
                }
            }
        }
    }

    @Test
    fun layout_keepsAMarginBetweenBlocks() {
        for ((w, h) in fields) {
            layouts(w, h).forEach { l ->
                for (i in l.indices) for (j in i + 1 until l.size) {
                    assertTrue("sem folga em $w × $h", chebyshev(l[i], l[j]) >= block + 8f - 1e-3f)
                }
            }
        }
    }

    @Test
    fun layout_isIrregular_notAGrid() {
        // Numa grade 3 × 3 (ou grade com ruído) cada bloco cai numa célula própria; ao acaso é raro.
        val (w, h) = 360f to 480f
        val oneBlockPerCell = layouts(w, h).count { l ->
            l.map { (it.x / (w / 3)).toInt().coerceAtMost(2) to (it.y / (h / 3)).toInt().coerceAtMost(2) }
                .toSet().size == CorsiRules.BLOCK_COUNT
        }
        assertTrue("$oneBlockPerCell de 300 layouts em grade", oneBlockPerCell < 15)
    }

    @Test
    fun layout_changesFromOneSeedToTheNext() {
        val (a, b) = layouts(360f, 480f, 1..2)
        assertNotEquals(a, b)
    }

    @Test
    fun layout_sameSeed_givesTheSameLayout() {
        assertEquals(
            CorsiRules.layout(Random(7), 360f, 480f, block),
            CorsiRules.layout(Random(7), 360f, 480f, block),
        )
    }

    @Test
    fun layout_isInReadingOrder() {
        layouts(360f, 480f).forEach { l ->
            val rows = l.map { (it.y / block).roundToInt() }
            assertEquals(rows.sorted(), rows)
            rows.distinct().forEach { row ->
                val xs = l.filter { (it.y / block).roundToInt() == row }.map { it.x }
                assertEquals(xs.sorted(), xs)
            }
        }
    }

    @Test
    fun layout_fitsATightField_withTheFallbackGrid() {
        // 200 × 200 dp: 9 blocos de 64 dp só cabem numa grade 3 × 3 deslocada.
        layouts(200f, 200f).forEach { l ->
            assertEquals(CorsiRules.BLOCK_COUNT, l.size)
            l.forEach {
                assertTrue(it.x >= block / 2 - 1e-3f && it.x <= 200f - block / 2 + 1e-3f)
                assertTrue(it.y >= block / 2 - 1e-3f && it.y <= 200f - block / 2 + 1e-3f)
            }
            for (i in l.indices) for (j in i + 1 until l.size) {
                assertTrue(chebyshev(l[i], l[j]) >= block - 1e-3f)
            }
        }
    }

    @Test
    fun blockSizeFor_staysBetween64And88_inPhoneSizedFields() {
        for ((w, h) in fields) {
            val size = CorsiRules.blockSizeFor(w, h)
            assertTrue("$w × $h → $size", size in CorsiRules.MIN_BLOCK_DP..CorsiRules.MAX_BLOCK_DP)
        }
        assertEquals(72f, CorsiRules.blockSizeFor(360f, 480f), 1e-3f)
    }

    @Test
    fun blockSizeFor_alwaysLeavesRoomForTheLayout() {
        for (w in 150..900 step 25) for (h in 150..900 step 25) {
            val size = CorsiRules.blockSizeFor(w.toFloat(), h.toFloat())
            // Nunca acima do que o campo comporta (3 × 3) e sempre aceito por layout().
            val l = CorsiRules.layout(Random(w * 1000 + h), w.toFloat(), h.toFloat(), size)
            assertEquals(CorsiRules.BLOCK_COUNT, l.size)
        }
    }

    @Test
    fun blockSizeFor_neverBreaksLayout_withAwkwardFieldSizes() {
        // Medidas quebradas (dp reais de telas e janelas), onde "campo / 3 * 3" perde no arredondamento.
        val random = Random(11)
        repeat(2_000) {
            val w = 60f + random.nextFloat() * 900f
            val h = 60f + random.nextFloat() * 900f
            val size = CorsiRules.blockSizeFor(w, h)
            assertEquals(CorsiRules.BLOCK_COUNT, CorsiRules.layout(random, w, h, size).size)
        }
    }

    @Test
    fun blockSizeFor_keepsTheLayoutIrregular_inPhoneSizedFields() {
        // Bloco na escolha certa: o sorteio livre fecha e quase nunca cai numa grade 3 × 3.
        for ((w, h) in listOf(360f to 480f, 411f to 700f, 320f to 480f)) {
            val size = CorsiRules.blockSizeFor(w, h)
            val grid = (0 until 300).count { seed ->
                CorsiRules.layout(Random(seed), w, h, size)
                    .map { (it.x / (w / 3)).toInt().coerceAtMost(2) to (it.y / (h / 3)).toInt().coerceAtMost(2) }
                    .toSet().size == CorsiRules.BLOCK_COUNT
            }
            assertTrue("$w × $h com bloco $size: $grid de 300 em grade", grid < 30)
        }
    }

    @Test
    fun layout_rejectsAFieldTooSmallForNineBlocks() {
        assertThrows(IllegalArgumentException::class.java) { CorsiRules.layout(Random(1), 150f, 400f, block) }
        assertThrows(IllegalArgumentException::class.java) { CorsiRules.layout(Random(1), 400f, 150f, block) }
    }

    // ---- Sequência ----

    @Test
    fun sequence_hasTheRequestedLengthWithDistinctBlocks() {
        val layout = CorsiRules.layout(Random(3), 360f, 480f, block)
        val random = Random(4)
        for (length in 1..CorsiRules.BLOCK_COUNT) repeat(100) {
            val s = CorsiRules.sequence(random, layout, length, block)
            assertEquals(length, s.size)
            assertEquals(length, s.toSet().size)
            assertTrue(s.all { it in layout.indices })
        }
    }

    @Test
    fun sequence_rejectsImpossibleLengths() {
        val layout = CorsiRules.layout(Random(3), 360f, 480f, block)
        assertThrows(IllegalArgumentException::class.java) { CorsiRules.sequence(Random(1), layout, 0, block) }
        assertThrows(IllegalArgumentException::class.java) { CorsiRules.sequence(Random(1), layout, 10, block) }
    }

    @Test
    fun sequence_avoidsThreeInARow_inAlmostEverySequence() {
        var withStraight = 0
        var total = 0
        for (seed in 0 until 100) {
            val layout = CorsiRules.layout(Random(seed), 360f, 480f, block)
            val random = Random(seed + 1_000)
            for (length in 3..CorsiRules.BLOCK_COUNT) {
                total++
                val s = CorsiRules.sequence(random, layout, length, block)
                if (CorsiRules.straightTriples(s, layout, block) > 0) withStraight++
            }
        }
        assertTrue("$withStraight de $total sequências com três em linha", withStraight <= total * 0.02)
    }

    @Test
    fun straightTriples_seesALineThroughTheMiddleBlockOnly() {
        val layout = listOf(BlockCenter(0f, 0f), BlockCenter(100f, 0f), BlockCenter(200f, 0f), BlockCenter(100f, 100f))
        assertEquals(1, CorsiRules.straightTriples(listOf(0, 1, 2), layout, block)) // reta A → B → C
        assertEquals(0, CorsiRules.straightTriples(listOf(0, 3, 2), layout, block)) // faz um V
        assertEquals(0, CorsiRules.straightTriples(listOf(0, 2, 1), layout, block)) // vai e volta
        assertEquals(0, CorsiRules.straightTriples(listOf(0, 1, 3), layout, block)) // dobra em B
        assertEquals(0, CorsiRules.straightTriples(listOf(0, 1), layout, block))
    }

    @Test
    fun straightTriples_toleratesHalfABlockOffTheLine() {
        val near = listOf(BlockCenter(0f, 0f), BlockCenter(100f, 20f), BlockCenter(200f, 0f))
        val far = listOf(BlockCenter(0f, 0f), BlockCenter(100f, 60f), BlockCenter(200f, 0f))
        assertEquals(1, CorsiRules.straightTriples(listOf(0, 1, 2), near, block))
        assertEquals(0, CorsiRules.straightTriples(listOf(0, 1, 2), far, block))
    }

    // ---- Resposta ----

    @Test
    fun attempt_correctTaps_endInCompleteOnTheLastOne() {
        val a = CorsiAttempt(listOf(4, 1, 7))
        assertEquals(TapResult.CORRECT, a.tap(4))
        assertEquals(1, a.progress)
        assertEquals(TapResult.CORRECT, a.tap(1))
        assertFalse(a.isFinished)
        assertEquals(TapResult.COMPLETE, a.tap(7))
        assertTrue(a.isFinished)
        assertEquals(3, a.progress)
    }

    @Test
    fun attempt_firstOutOfOrderTapEndsInError() {
        val a = CorsiAttempt(listOf(4, 1, 7))
        assertEquals(TapResult.CORRECT, a.tap(4))
        assertEquals(TapResult.WRONG, a.tap(7))
        assertTrue(a.isFinished)
        assertEquals(1, a.progress)
    }

    @Test
    fun attempt_repeatingABlockAlreadyTapped_isAnError() {
        val a = CorsiAttempt(listOf(4, 1, 7))
        a.tap(4)
        assertEquals(TapResult.WRONG, a.tap(4))
    }

    @Test
    fun attempt_tapsAfterTheEnd_areIgnored() {
        val wrong = CorsiAttempt(listOf(2, 3, 5))
        wrong.tap(9)
        assertEquals(TapResult.IGNORED, wrong.tap(2))
        val done = CorsiAttempt(listOf(2))
        assertEquals(TapResult.COMPLETE, done.tap(2))
        assertEquals(TapResult.IGNORED, done.tap(2))
        assertEquals(1, done.progress)
    }

    @Test
    fun attempt_rejectsAnEmptySequence() {
        assertThrows(IllegalArgumentException::class.java) { CorsiAttempt(emptyList()) }
    }
}
