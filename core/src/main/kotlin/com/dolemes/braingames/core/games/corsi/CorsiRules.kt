package com.dolemes.braingames.core.games.corsi

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Centro de um bloco, em dp, no sistema de coordenadas do campo de jogo (origem no canto superior esquerdo). */
data class BlockCenter(val x: Float, val y: Float)

/** Resultado de um toque numa resposta em andamento. */
enum class TapResult {
    /** Bloco certo; ainda faltam blocos. */
    CORRECT,

    /** Último bloco certo: a sequência foi reproduzida. */
    COMPLETE,

    /** Primeiro toque fora de ordem: a tentativa acaba em erro. */
    WRONG,

    /** A tentativa já terminou; o toque não conta. */
    IGNORED,
}

/**
 * A resposta do jogador a uma sequência. A tentativa termina no primeiro toque fora de ordem
 * (erro) ou no último toque certo (acerto); depois disso os toques são ignorados.
 */
class CorsiAttempt(val sequence: List<Int>) {
    init {
        require(sequence.isNotEmpty()) { "A sequência não pode ser vazia." }
    }

    /** Quantos blocos o jogador já acertou nesta resposta. */
    var progress: Int = 0
        private set

    var isFinished: Boolean = false
        private set

    fun tap(block: Int): TapResult {
        if (isFinished) return TapResult.IGNORED
        if (block != sequence[progress]) {
            isFinished = true
            return TapResult.WRONG
        }
        progress++
        if (progress < sequence.size) return TapResult.CORRECT
        isFinished = true
        return TapResult.COMPLETE
    }
}

/**
 * Regras puras da Sequência de Luzes (blocos de Corsi; Corsi, 1972; Kessels et al., 2000).
 * Níveis 1 a 14: a sequência cresce de 3 para 9 blocos e o tempo aceso encurta.
 */
object CorsiRules {
    const val MAX_LEVEL = 14
    const val BLOCK_COUNT = 9

    /** Intervalo entre um bloco apagar e o próximo acender, em ms. */
    const val GAP_BETWEEN_BLOCKS_MS = 250L

    /** Dois toques no mesmo bloco em menos que isto contam como um só. */
    const val DOUBLE_TAP_MS = 200L

    /** Menor bloco (exigência de acessibilidade do GDD) e o maior que vale a pena desenhar. */
    const val MIN_BLOCK_DP = 64f
    const val MAX_BLOCK_DP = 88f

    /** Posições possíveis que o campo deve ter para o sorteio dos 9 blocos ficar irregular (~40% de ocupação). */
    private const val COMFORTABLE_SLOTS = 22

    /** Folga entre blocos vizinhos, em dp, além do tamanho do bloco. */
    private const val MARGIN_DP = 8f
    private const val FIT_TOLERANCE_DP = 0.01f // absorve o arredondamento de "campo / 3 * 3"
    private const val LAYOUT_ATTEMPTS = 60
    private const val POINT_ATTEMPTS = 300
    private const val SEQUENCE_ATTEMPTS = 200

    /** Blocos na sequência: 2 + ⌈nível / 2⌉ (3 no nível 1, 9 nos níveis 13 e 14). */
    fun sequenceLength(level: Int): Int = 2 + (level.coerceIn(1, MAX_LEVEL) + 1) / 2

    /** Tempo que cada bloco fica aceso, em ms: max(450, 800 − 20 · nível). */
    fun litMillis(level: Int): Long = max(450, 800 - 20 * level.coerceIn(1, MAX_LEVEL)).toLong()

    /** Pontos-base de uma sequência certa: 10 por bloco (o nível escala depois, no motor). */
    fun basePoints(sequenceLength: Int): Int = 10 * sequenceLength

    /**
     * Tamanho do bloco para o campo: o maior entre [MIN_BLOCK_DP] e [MAX_BLOCK_DP] que ainda deixa
     * folga para um sorteio irregular (blocos grandes demais empurram tudo para uma grade).
     * Só num campo minúsculo o bloco encolhe abaixo do mínimo, para caberem os 9.
     */
    fun blockSizeFor(widthDp: Float, heightDp: Float): Float {
        var size = MAX_BLOCK_DP
        while (size > MIN_BLOCK_DP && slotsThatFit(widthDp, heightDp, size) < COMFORTABLE_SLOTS) size -= 4f
        return min(max(size, MIN_BLOCK_DP), min(widthDp, heightDp) / 3)
    }

    private fun slotsThatFit(widthDp: Float, heightDp: Float, blockDp: Float): Int {
        val step = blockDp + MARGIN_DP
        val columns = ((widthDp - blockDp) / step).toInt() + 1
        val rows = ((heightDp - blockDp) / step).toInt() + 1
        return max(0, columns) * max(0, rows)
    }

    /**
     * Sorteia as posições dos 9 blocos no campo [widthDp] × [heightDp], em ordem de leitura
     * (de cima para baixo, da esquerda para a direita), que é também a ordem do TalkBack.
     * Irregulares (nunca em grade), inteiros dentro do campo e sem se tocar. Se o sorteio livre
     * não fechar (campo apertado), cai numa grade 3 × 3 com deslocamento aleatório, que sempre cabe.
     */
    fun layout(random: Random, widthDp: Float, heightDp: Float, blockDp: Float): List<BlockCenter> {
        require(blockDp > 0f) { "blockDp deve ser positivo." }
        require(widthDp + FIT_TOLERANCE_DP >= 3 * blockDp && heightDp + FIT_TOLERANCE_DP >= 3 * blockDp) {
            "O campo (${widthDp} × ${heightDp} dp) não comporta 9 blocos de $blockDp dp."
        }
        val minDistance = blockDp + MARGIN_DP
        val half = blockDp / 2
        val spanX = widthDp - blockDp
        val spanY = heightDp - blockDp
        repeat(LAYOUT_ATTEMPTS) {
            val points = ArrayList<BlockCenter>(BLOCK_COUNT)
            var tries = 0
            while (points.size < BLOCK_COUNT && tries++ < POINT_ATTEMPTS) {
                val p = BlockCenter(half + random.nextFloat() * spanX, half + random.nextFloat() * spanY)
                if (points.all { chebyshev(it, p) >= minDistance }) points += p
            }
            if (points.size == BLOCK_COUNT) return readingOrder(points, blockDp)
        }
        return readingOrder(jitteredGrid(random, widthDp, heightDp, blockDp), blockDp)
    }

    /** Uma célula de 3 × 3 por bloco, com o bloco em posição aleatória dentro da própria célula. */
    private fun jitteredGrid(random: Random, widthDp: Float, heightDp: Float, blockDp: Float): List<BlockCenter> {
        val cellW = widthDp / 3
        val cellH = heightDp / 3
        val half = blockDp / 2
        // Folga entre células vizinhas: a margem inteira se a célula deixar, senão o que sobrar.
        val gapX = min(MARGIN_DP, cellW - blockDp)
        val gapY = min(MARGIN_DP, cellH - blockDp)
        return (0 until BLOCK_COUNT).map { i ->
            val col = i % 3
            val row = i / 3
            BlockCenter(
                x = col * cellW + half + gapX / 2 + random.nextFloat() * (cellW - blockDp - gapX),
                y = row * cellH + half + gapY / 2 + random.nextFloat() * (cellH - blockDp - gapY),
            )
        }
    }

    /** Ordem de leitura: agrupa em faixas de altura de um bloco e ordena cada faixa da esquerda para a direita. */
    private fun readingOrder(points: List<BlockCenter>, blockDp: Float): List<BlockCenter> =
        points.sortedWith(compareBy({ (it.y / blockDp).roundToInt() }, { it.x }))

    /**
     * Sorteia a sequência: [length] blocos distintos, evitando três blocos seguidos em linha reta
     * (o que viraria um padrão geométrico fácil de memorizar). [layout] são as posições da rodada.
     * Devolve os índices dos blocos, na ordem em que acendem.
     */
    fun sequence(random: Random, layout: List<BlockCenter>, length: Int, blockDp: Float): List<Int> {
        require(length in 1..layout.size) { "length deve estar entre 1 e ${layout.size}." }
        var best = layout.indices.shuffled(random).take(length)
        var bestScore = straightTriples(best, layout, blockDp)
        var attempts = 1
        while (bestScore > 0 && attempts++ < SEQUENCE_ATTEMPTS) {
            val candidate = layout.indices.shuffled(random).take(length)
            val score = straightTriples(candidate, layout, blockDp)
            if (score < bestScore) {
                best = candidate
                bestScore = score
            }
        }
        return best
    }

    /**
     * Quantos trios seguidos da sequência formam uma linha reta: o do meio está entre os outros
     * dois e a menos de meio bloco da reta que os une.
     */
    fun straightTriples(sequence: List<Int>, layout: List<BlockCenter>, blockDp: Float): Int =
        (0..sequence.size - 3).count { i ->
            isStraight(layout[sequence[i]], layout[sequence[i + 1]], layout[sequence[i + 2]], blockDp / 2)
        }

    private fun isStraight(a: BlockCenter, b: BlockCenter, c: BlockCenter, toleranceDp: Float): Boolean {
        val acX = c.x - a.x
        val acY = c.y - a.y
        val lengthSquared = acX * acX + acY * acY
        if (lengthSquared == 0f) return false
        // Posição de b projetada sobre a reta a→c (0 = em a, 1 = em c) e distância de b até a reta.
        val t = ((b.x - a.x) * acX + (b.y - a.y) * acY) / lengthSquared
        val cross = (b.x - a.x) * acY - (b.y - a.y) * acX
        val distance = abs(cross) / sqrt(lengthSquared)
        return t > 0f && t < 1f && distance < toleranceDp
    }

    private fun chebyshev(a: BlockCenter, b: BlockCenter): Float = max(abs(a.x - b.x), abs(a.y - b.y))
}
