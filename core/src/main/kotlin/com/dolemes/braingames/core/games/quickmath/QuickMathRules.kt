package com.dolemes.braingames.core.games.quickmath

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Uma conta com quatro opções de resposta.
 *
 * @property text a conta pronta para exibir, ex.: "3 + 4" ou "12 × 4 − 18" (sinais tipográficos).
 * @property options as 4 opções, distintas e sem negativos, na ordem em que aparecem na tela.
 */
data class MathProblem(val text: String, val answer: Int, val options: List<Int>) {
    val correctIndex: Int get() = options.indexOf(answer)
}

/**
 * Regras puras do Cálculo Relâmpago (aritmética mental; Ashcraft, 1992): nível → tipo de conta,
 * sorteio e distratores plausíveis. Níveis 1 a 20:
 * 1–3 soma até 10 · 4–6 soma até 20 e subtração · 7–9 dois dígitos ± um dígito ·
 * 10–12 tabuada até 5 e dois dígitos ± dois dígitos sem "vai um" · 13–15 tabuada até 10 e divisão
 * exata · 16–18 duas operações com precedência · 19–20 operações mistas com dois dígitos.
 */
object QuickMathRules {
    const val MAX_LEVEL = 20
    const val OPTION_COUNT = 4
    private const val NEAR_LIMIT = 20

    private const val MINUS = '−'
    private const val TIMES = '×'
    private const val DIVIDED = '÷'

    /** Uma conta sem as opções: o gabarito e os erros típicos de quem a resolve. */
    private class Draft(
        val text: String,
        val answer: Int,
        /** Erros típicos desta conta (operação trocada, tabuada vizinha...). Filtrados depois. */
        val typicalErrors: List<Int> = emptyList(),
        /** Resultado de ignorar a precedência (3 + 4 × 5 = 35). Sempre vira opção, se válido. */
        val precedenceSlip: Int? = null,
    )

    /**
     * Sorteia uma conta do nível. Passe [previous] para não repetir a mesma conta em seguida.
     * Toda a aleatoriedade vem de [random].
     */
    fun next(random: Random, level: Int, previous: MathProblem? = null): MathProblem {
        var draft = draft(random, level)
        var retries = 0
        while (draft.text == previous?.text && retries++ < 8) draft = draft(random, level)
        return MathProblem(draft.text, draft.answer, options(random, draft))
    }

    /** Contas por minuto, arredondado; 0 se a rodada não teve duração. */
    fun problemsPerMinute(trials: Int, durationSeconds: Double): Int =
        if (durationSeconds <= 0.0) 0 else (trials * 60.0 / durationSeconds).roundToInt()

    private fun draft(random: Random, level: Int): Draft = when (level.coerceIn(1, MAX_LEVEL)) {
        in 1..3 -> smallSum(random, level)
        in 4..6 -> sumOrDifferenceTo20(random, level)
        in 7..9 -> twoDigitByOneDigit(random, level)
        in 10..12 -> tablesTo5OrTwoDigitsWithoutCarry(random)
        in 13..15 -> tablesTo10OrExactDivision(random, level)
        in 16..18 -> precedence(random, level)
        else -> mixedTwoDigits(random, level)
    }

    // ---- Níveis 1–3: soma com resultado até 10 (6, 8 e 10) ----
    private fun smallSum(random: Random, level: Int): Draft {
        val maxSum = 4 + 2 * level.coerceIn(1, 3)
        val sum = random.between(3, maxSum)
        val a = random.between(1, sum - 1)
        val b = sum - a
        return Draft("$a + $b", sum, typicalErrors = listOf(a - b))
    }

    // ---- Níveis 4–6: somas até 12, 16 e 20; subtração sem negativos ----
    private fun sumOrDifferenceTo20(random: Random, level: Int): Draft {
        val limit = 12 + 4 * (level.coerceIn(4, 6) - 4)
        return if (random.nextBoolean()) {
            val sum = random.between(4, limit)
            val a = random.between(2, sum - 2)
            val b = sum - a
            Draft("$a + $b", sum, typicalErrors = listOf(a - b))
        } else {
            val a = random.between(6, limit)
            val b = random.between(2, a - 1)
            Draft("$a $MINUS $b", a - b, typicalErrors = listOf(a + b))
        }
    }

    // ---- Níveis 7–9: dois dígitos ± um dígito (até 49, 74 e 99) ----
    private fun twoDigitByOneDigit(random: Random, level: Int): Draft {
        val maxA = 49 + 25 * (level.coerceIn(7, 9) - 7)
        val a = random.between(10, maxA)
        val b = random.between(2, 9)
        return if (random.nextBoolean()) {
            Draft("$a + $b", a + b, typicalErrors = listOf(a - b))
        } else {
            Draft("$a $MINUS $b", a - b, typicalErrors = listOf(a + b))
        }
    }

    // ---- Níveis 10–12: tabuada até 5; dois dígitos ± dois dígitos sem "vai um" nem "empresta um" ----
    private fun tablesTo5OrTwoDigitsWithoutCarry(random: Random): Draft = when (random.nextInt(3)) {
        0 -> multiplication(random.between(2, 5), random.between(2, 5))
        1 -> {
            val tensA = random.between(1, 8)
            val tensB = random.between(1, 9 - tensA)
            val unitsA = random.between(1, 8)
            val unitsB = random.between(1, 9 - unitsA)
            val a = 10 * tensA + unitsA
            val b = 10 * tensB + unitsB
            Draft("$a + $b", a + b, typicalErrors = listOf(a - b))
        }
        else -> {
            val tensA = random.between(2, 9)
            val tensB = random.between(1, tensA - 1)
            val unitsA = random.between(1, 9)
            val unitsB = random.between(1, unitsA)
            val a = 10 * tensA + unitsA
            val b = 10 * tensB + unitsB
            Draft("$a $MINUS $b", a - b, typicalErrors = listOf(a + b))
        }
    }

    // ---- Níveis 13–15: tabuada até 8, 9 e 10; divisão exata ----
    private fun tablesTo10OrExactDivision(random: Random, level: Int): Draft {
        val maxFactor = 8 + (level.coerceIn(13, 15) - 13)
        val a = random.between(3, maxFactor)
        val b = random.between(3, maxFactor)
        return if (random.nextBoolean()) {
            multiplication(a, b)
        } else {
            Draft("${a * b} $DIVIDED $b", a, typicalErrors = listOf(b))
        }
    }

    private fun multiplication(a: Int, b: Int): Draft =
        Draft("$a $TIMES $b", a * b, typicalErrors = listOf(a + b, a * (b + 1), a * (b - 1)))

    // ---- Níveis 16–18: duas operações, respeitando a precedência ----
    private fun precedence(random: Random, level: Int): Draft {
        val step = level.coerceIn(16, 18) - 16
        val maxFactor = 5 + 2 * step
        val b = random.between(2, maxFactor)
        val c = random.between(2, maxFactor)
        return when (random.nextInt(3)) {
            0 -> {
                val a = random.between(2, 9 + 3 * step)
                Draft("$a + $b $TIMES $c", a + b * c, precedenceSlip = (a + b) * c)
            }
            1 -> {
                val a = random.between(b * c + 1, b * c + 12)
                Draft("$a $MINUS $b $TIMES $c", a - b * c, precedenceSlip = (a - b) * c)
            }
            else -> {
                val z = random.between(2, 9 + 3 * step)
                Draft("$b $TIMES $c + $z", b * c + z, precedenceSlip = b * (c + z))
            }
        }
    }

    // ---- Níveis 19–20: operações mistas com dois dígitos ----
    private fun mixedTwoDigits(random: Random, level: Int): Draft {
        val maxA = if (level >= MAX_LEVEL) 25 else 19
        return when (random.nextInt(4)) {
            0 -> {
                val a = random.between(11, maxA)
                val b = random.between(3, 9)
                val c = random.between(10, minOf(99, a * b - 1))
                Draft("$a $TIMES $b $MINUS $c", a * b - c, typicalErrors = listOf(a * b + c))
            }
            1 -> {
                val a = random.between(11, maxA)
                val b = random.between(3, 9)
                val c = random.between(10, 99)
                Draft("$a $TIMES $b + $c", a * b + c, typicalErrors = listOf(a * b - c))
            }
            2 -> {
                val a = random.between(10, 99)
                val b = random.between(3, 9)
                val c = random.between(3, 12)
                Draft("$a + $b $TIMES $c", a + b * c, precedenceSlip = (a + b) * c)
            }
            else -> {
                val b = random.between(3, 9)
                val c = random.between(3, 10)
                val a = random.between(b * c + 1, 99)
                Draft("$a $MINUS $b $TIMES $c", a - b * c, precedenceSlip = (a - b) * c)
            }
        }
    }

    /**
     * Três distratores plausíveis + a resposta, em ordem sorteada. Nunca repete valor nem devolve
     * negativo. Para ninguém acertar só por eliminação, prefere (1) mesma paridade e mesmo número
     * de dígitos que a resposta, depois (2) só o mesmo número de dígitos (é aí que entra o ±1),
     * (3) o valor de mesmo tamanho mais próximo e por último (4) qualquer valor. A única exceção é o resultado de ignorar a precedência: ele
     * sempre entra, porque é o erro que este jogo quer pegar.
     */
    private fun options(random: Random, draft: Draft): List<Int> {
        val answer = draft.answer
        val chosen = LinkedHashSet<Int>()
        fun usable(v: Int) = v >= 0 && v != answer && v !in chosen

        draft.precedenceSlip?.takeIf { usable(it) }?.let { chosen += it }

        val typical = (draft.typicalErrors + listOfNotNull(reversedDigits(answer))).distinct()
        val nearby = listOf(answer + 2, answer - 2, answer + 10, answer - 10, answer + 1, answer - 1)
        val candidates = (typical + nearby).distinct()
        val closestSameDigits = (1..NEAR_LIMIT).flatMap { listOf(answer + it, answer - it) }
            .filter { sameDigits(it, answer) }
        val tiers = listOf(
            candidates.filter { sameDigits(it, answer) && (it - answer) % 2 == 0 }.shuffled(random),
            candidates.filter { sameDigits(it, answer) }.shuffled(random),
            closestSameDigits, // já em ordem de proximidade: não embaralhar
            candidates.shuffled(random),
        )
        for (tier in tiers) {
            for (v in tier) {
                if (chosen.size == OPTION_COUNT - 1) break
                if (usable(v)) chosen += v
            }
        }
        var distance = 1
        while (chosen.size < OPTION_COUNT - 1) { // só para respostas muito pequenas
            for (v in listOf(answer + distance, answer - distance)) {
                if (chosen.size < OPTION_COUNT - 1 && usable(v)) chosen += v
            }
            distance++
        }
        return (chosen + answer).shuffled(random)
    }

    private fun sameDigits(value: Int, answer: Int): Boolean =
        value >= 0 && value.toString().length == answer.toString().length

    /** 54 → 45; null quando não há troca com sentido (um dígito só, ou zero no fim: 10 → "01"). */
    private fun reversedDigits(value: Int): Int? =
        if (value < 10 || value % 10 == 0) null else value.toString().reversed().toInt()

    private fun Random.between(min: Int, max: Int): Int = nextInt(min, max + 1)
}
