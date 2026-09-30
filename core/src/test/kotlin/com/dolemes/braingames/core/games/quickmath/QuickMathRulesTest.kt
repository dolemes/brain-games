package com.dolemes.braingames.core.games.quickmath

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickMathRulesTest {

    private val samplesPerLevel = 2_000

    private fun problems(level: Int, seed: Int = level): List<MathProblem> {
        val random = Random(seed)
        return (1..samplesPerLevel).map { QuickMathRules.next(random, level) }
    }

    private fun operators(text: String): List<String> = text.split(" ").filterIndexed { i, _ -> i % 2 == 1 }

    /** Avalia a conta com a precedência usual, exigindo divisão exata (independe do gerador). */
    private fun evaluate(text: String): Int {
        val tokens = text.split(" ")
        val numbers = mutableListOf(tokens[0].toInt())
        val addOps = mutableListOf<String>()
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val n = tokens[i + 1].toInt()
            when (op) {
                "×" -> numbers[numbers.lastIndex] = numbers.last() * n
                "÷" -> {
                    assertEquals("divisão com resto em '$text'", 0, numbers.last() % n)
                    numbers[numbers.lastIndex] = numbers.last() / n
                }
                else -> { addOps += op; numbers += n }
            }
            i += 2
        }
        var total = numbers[0]
        addOps.forEachIndexed { k, op -> total = if (op == "+") total + numbers[k + 1] else total - numbers[k + 1] }
        return total
    }

    @Test
    fun answerMatchesTheText_atEveryLevel() {
        for (level in 1..QuickMathRules.MAX_LEVEL) {
            problems(level).forEach { p ->
                assertEquals("nível $level: '${p.text}'", evaluate(p.text), p.answer)
            }
        }
    }

    @Test
    fun options_areFourDistinctNonNegativeAndContainTheAnswer() {
        for (level in 1..QuickMathRules.MAX_LEVEL) {
            problems(level).forEach { p ->
                val msg = "nível $level: '${p.text}' ${p.options}"
                assertEquals(msg, QuickMathRules.OPTION_COUNT, p.options.size)
                assertEquals(msg, QuickMathRules.OPTION_COUNT, p.options.toSet().size)
                assertTrue(msg, p.options.all { it >= 0 })
                assertTrue(msg, p.answer in p.options)
                assertEquals(msg, p.answer, p.options[p.correctIndex])
            }
        }
    }

    @Test
    fun noNegativeAnswersAndNoRemainders_atEveryLevel() {
        for (level in 1..QuickMathRules.MAX_LEVEL) {
            problems(level).forEach { p -> assertTrue("nível $level: '${p.text}'", p.answer >= 0) }
        }
    }

    @Test
    fun levels1to3_areSumsUpTo10() {
        for (level in 1..3) {
            problems(level).forEach { p ->
                assertEquals(listOf("+"), operators(p.text))
                assertTrue("'${p.text}'", p.answer <= 10)
            }
        }
        assertTrue(problems(1).all { it.answer <= 6 })
    }

    @Test
    fun levels4to6_areAdditionsUpTo20OrSubtractions() {
        for (level in 4..6) {
            problems(level).forEach { p ->
                val ops = operators(p.text)
                assertEquals(1, ops.size)
                assertTrue(ops.single() == "+" || ops.single() == "−")
                assertTrue("'${p.text}'", p.answer in 1..20)
                if (ops.single() == "+") assertTrue(p.answer <= 20)
            }
        }
    }

    @Test
    fun levels7to9_areTwoDigitsWithOneDigit() {
        for (level in 7..9) {
            problems(level).forEach { p ->
                val t = p.text.split(" ")
                assertTrue("'${p.text}'", t[0].toInt() in 10..99)
                assertTrue("'${p.text}'", t[2].toInt() in 2..9)
                assertTrue(t[1] == "+" || t[1] == "−")
            }
        }
    }

    @Test
    fun levels10to12_haveNoCarryAndNoDivision() {
        for (level in 10..12) {
            val list = problems(level)
            list.forEach { p ->
                val t = p.text.split(" ")
                assertNotEquals("÷", t[1])
                val a = t[0].toInt()
                val b = t[2].toInt()
                when (t[1]) {
                    "×" -> { assertTrue(a in 2..5 && b in 2..5) }
                    "+" -> assertTrue("vai um em '${p.text}'", a % 10 + b % 10 <= 9 && a / 10 + b / 10 <= 9)
                    "−" -> assertTrue("empresta um em '${p.text}'", a % 10 >= b % 10 && a / 10 > b / 10)
                }
            }
            assertTrue(list.any { operators(it.text).single() == "×" })
            assertTrue(list.any { operators(it.text).single() == "+" })
            assertTrue(list.any { operators(it.text).single() == "−" })
        }
    }

    @Test
    fun levels13to15_haveTablesAndExactDivision() {
        for (level in 13..15) {
            val list = problems(level)
            list.forEach { p ->
                assertEquals(1, operators(p.text).size)
                assertTrue(operators(p.text).single() in listOf("×", "÷"))
            }
            assertTrue(list.any { operators(it.text).single() == "÷" })
            val maxFactor = 8 + (level - 13)
            list.filter { operators(it.text).single() == "×" }.forEach { p ->
                val t = p.text.split(" ")
                assertTrue(t[0].toInt() <= maxFactor && t[2].toInt() <= maxFactor)
            }
        }
    }

    @Test
    fun levels16to20_haveTwoOperations() {
        for (level in 16..20) {
            problems(level).forEach { p -> assertEquals("nível $level: '${p.text}'", 2, operators(p.text).size) }
        }
    }

    @Test
    fun levels16to20_alwaysOfferTheIgnoredPrecedenceResult() {
        for (level in 16..20) {
            problems(level).forEach { p ->
                val t = p.text.split(" ")
                val leftToRight = leftToRight(t)
                if (leftToRight != p.answer && leftToRight >= 0) {
                    assertTrue("nível $level: '${p.text}' ${p.options}", leftToRight in p.options)
                }
            }
        }
    }

    private fun leftToRight(t: List<String>): Int {
        var total = t[0].toInt()
        var i = 1
        while (i < t.size) {
            val n = t[i + 1].toInt()
            total = when (t[i]) { "+" -> total + n; "−" -> total - n; "×" -> total * n; else -> total / n }
            i += 2
        }
        return total
    }

    @Test
    fun problemsUseTypographicSymbols() {
        for (level in 1..QuickMathRules.MAX_LEVEL) {
            problems(level).forEach { p ->
                assertTrue(p.text, !p.text.contains('-') && !p.text.contains('*') && !p.text.contains('/'))
            }
        }
    }

    @Test
    fun correctAnswerPosition_isShuffled() {
        val positions = problems(10).groupingBy { it.correctIndex }.eachCount()
        assertEquals(QuickMathRules.OPTION_COUNT, positions.size)
        positions.values.forEach { assertTrue("posição enviesada: $positions", it > samplesPerLevel * 0.18) }
    }

    @Test
    fun distractors_preferTheParityOfTheAnswer_whenThereIsNoPrecedenceSlip() {
        for (level in 7..15) {
            var same = 0
            var total = 0
            problems(level).forEach { p ->
                p.options.filter { it != p.answer }.forEach {
                    total++
                    if ((it - p.answer) % 2 == 0) same++
                }
            }
            assertTrue("nível $level: só ${same * 100 / total}% com a mesma paridade", same.toDouble() / total > 0.7)
        }
    }

    @Test
    fun distractors_haveTheSameNumberOfDigitsAsTheAnswer_upToLevel15() {
        for (level in 1..15) {
            var same = 0
            var total = 0
            problems(level).forEach { p ->
                p.options.filter { it != p.answer }.forEach {
                    total++
                    if (it.toString().length == p.answer.toString().length) same++
                }
            }
            assertTrue("nível $level: só ${same * 100 / total}% do mesmo tamanho", same.toDouble() / total > 0.95)
        }
    }

    @Test
    fun answersEndingInZero_neverOfferTheMeaninglessReversal() {
        val tens = (1..3).flatMap { problems(it) }.filter { it.answer == 10 }
        assertTrue(tens.isNotEmpty())
        tens.forEach { assertTrue("'${it.text}' ${it.options}", 1 !in it.options) }
    }

    @Test
    fun smallSums_areNotAlwaysPlusOne() {
        val second = problems(1).map { it.text.split(" ")[2].toInt() }.groupingBy { it }.eachCount()
        assertTrue("segundo operando enviesado: $second", second.getValue(1) < samplesPerLevel * 0.45)
    }

    @Test
    fun sameSeed_givesTheSameProblems() {
        assertEquals(problems(12, seed = 99), problems(12, seed = 99))
        assertNotEquals(problems(12, seed = 99), problems(12, seed = 100))
    }

    @Test
    fun next_avoidsRepeatingThePreviousProblem() {
        val random = Random(5)
        var previous = QuickMathRules.next(random, 1)
        repeat(500) {
            val p = QuickMathRules.next(random, 1, previous)
            assertNotEquals(previous.text, p.text)
            previous = p
        }
    }

    @Test
    fun levelsOutsideTheRange_areClamped() {
        val random = Random(1)
        assertEquals(1, operators(QuickMathRules.next(random, -3).text).size)
        assertEquals(2, operators(QuickMathRules.next(random, 99).text).size)
    }

    @Test
    fun problemsPerMinute_scalesToOneMinute() {
        assertEquals(20, QuickMathRules.problemsPerMinute(trials = 30, durationSeconds = 90.0))
        assertEquals(0, QuickMathRules.problemsPerMinute(trials = 5, durationSeconds = 0.0))
    }
}
