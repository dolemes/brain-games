package com.dolemes.braingames.core

import java.time.LocalDate
import kotlin.random.Random

/** Entrada do catálogo usada pelo planejador (o app monta a lista a partir das definições). */
data class CatalogEntry(val id: String, val domain: CognitiveDomain)

/**
 * Monta o "treino do dia": jogos de domínios diferentes, priorizando os menos jogados.
 * O plano fica gravado no progresso e não muda até a meia-noite, mesmo que o jogador
 * jogue (e mude as contagens) no meio do dia.
 */
object DailyWorkoutPlanner {
    const val DEFAULT_GAMES_PER_DAY = 3

    /** Devolve o progresso com o treino de hoje garantido (o mesmo objeto se já existir). */
    fun ensureToday(
        progress: PlayerProgress,
        catalog: List<CatalogEntry>,
        today: LocalDate,
        gamesPerDay: Int = DEFAULT_GAMES_PER_DAY,
    ): PlayerProgress {
        val todayIso = today.toString()
        val available = catalog.map { it.id }.toSet()
        val current = progress.workout
        if (current.date == todayIso && current.gameIds.isNotEmpty() && available.containsAll(current.gameIds)) {
            return progress
        }
        return progress.copy(workout = Workout(todayIso, plan(catalog, progress, today, gamesPerDay)))
    }

    /** Gera um plano sem gravar nada (determinístico para a mesma data e o mesmo progresso). */
    fun plan(catalog: List<CatalogEntry>, progress: PlayerProgress, today: LocalDate, gamesPerDay: Int): List<String> {
        val rng = Random(today.year * 10000 + today.monthValue * 100 + today.dayOfMonth)
        val byDomain = catalog.groupBy { it.domain }
        val domains = byDomain.keys.sortedBy { it.ordinal }.shuffled(rng)

        val result = mutableListOf<String>()
        for (domain in domains) {
            if (result.size >= gamesPerDay) break
            result += pickLeastPlayed(byDomain.getValue(domain), progress, rng, result)
        }
        // Menos domínios que jogos pedidos: completa com os demais, menos jogados primeiro.
        while (result.size < gamesPerDay && result.size < catalog.size) {
            result += pickLeastPlayed(catalog, progress, rng, result)
        }
        return result
    }

    private fun pickLeastPlayed(
        options: List<CatalogEntry>,
        progress: PlayerProgress,
        rng: Random,
        exclude: List<String>,
    ): String {
        val candidates = options.filter { it.id !in exclude }
        check(candidates.isNotEmpty()) { "Nenhum jogo disponível para o treino." }
        val playCounts = candidates.associate { it.id to (progress.game(it.id)?.timesPlayed ?: 0) }
        val min = playCounts.values.min()
        val tied = candidates.map { it.id }.filter { playCounts.getValue(it) == min }
        return tied[rng.nextInt(tied.size)]
    }
}
