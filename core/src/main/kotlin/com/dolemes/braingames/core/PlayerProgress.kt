package com.dolemes.braingames.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

/** Resumo de uma rodada no histórico do jogo. Datas em ISO (yyyy-MM-dd). */
@Serializable
data class SessionRecord(
    val date: String,
    val score: Int,
    val endLevel: Int,
    val accuracy: Double,
)

/** Progresso de um minijogo. [gameId] é a chave do save: nunca mude depois de publicar. */
@Serializable
data class GameProgress(
    val gameId: String,
    val staircase: StaircaseState = StaircaseState(),
    val bestScore: Int = 0,
    val timesPlayed: Int = 0,
    val recent: List<SessionRecord> = emptyList(),
)

/** Treino do dia, fixado uma vez por dia pelo [DailyWorkoutPlanner]. */
@Serializable
data class Workout(
    val date: String = "",
    val gameIds: List<String> = emptyList(),
    val doneIds: List<String> = emptyList(),
) {
    fun isComplete(today: LocalDate): Boolean =
        date == today.toString() && gameIds.isNotEmpty() && doneIds.containsAll(gameIds)
}

/** Preferências de acessibilidade. O idioma fica com o Android (preferência por app). */
@Serializable
data class Preferences(
    val untimedMode: Boolean = false,
    val textScale: Float = 1f,
)

/**
 * Todo o progresso do jogador, salvo em JSON local (ver [ProgressJson]).
 * Imutável: cada mudança gera uma cópia, o que combina com StateFlow e Compose.
 * Todo campo tem valor padrão, para saves antigos (sem o campo) continuarem abrindo.
 */
@Serializable
data class PlayerProgress(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val preferences: Preferences = Preferences(),
    val completedSessions: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastPlayedDate: String = "",
    val workout: Workout = Workout(),
    val games: List<GameProgress> = emptyList(),
) {
    fun game(gameId: String): GameProgress? = games.firstOrNull { it.gameId == gameId }

    /**
     * Aplica o resultado de uma rodada. Rodada abandonada só guarda a escada (para não
     * perder o nível), sem contar sessão, recorde nem sequência de dias.
     */
    fun withResult(result: RoundResult, finalStaircase: StaircaseState?, today: LocalDate): PlayerProgress {
        val current = game(result.gameId) ?: GameProgress(result.gameId)
        val withStaircase = if (finalStaircase != null) current.copy(staircase = finalStaircase) else current
        if (!result.completed) return copy(games = replace(withStaircase))

        val todayIso = today.toString()
        val updatedGame = withStaircase.copy(
            timesPlayed = withStaircase.timesPlayed + 1,
            bestScore = maxOf(withStaircase.bestScore, result.score),
            recent = (withStaircase.recent + SessionRecord(todayIso, result.score, result.endLevel, result.accuracy))
                .takeLast(MAX_RECENT_PER_GAME),
        )
        val streak = nextStreak(today)
        val updatedWorkout =
            if (workout.date == todayIso && result.gameId in workout.gameIds && result.gameId !in workout.doneIds) {
                workout.copy(doneIds = workout.doneIds + result.gameId)
            } else {
                workout
            }

        return copy(
            games = replace(updatedGame),
            completedSessions = completedSessions + 1,
            currentStreakDays = streak,
            bestStreakDays = maxOf(bestStreakDays, streak),
            lastPlayedDate = todayIso,
            workout = updatedWorkout,
        )
    }

    private fun nextStreak(today: LocalDate): Int {
        if (lastPlayedDate == today.toString()) return maxOf(currentStreakDays, 1)
        val last = runCatching { LocalDate.parse(lastPlayedDate) }.getOrNull()
        val playedYesterday = last != null && ChronoUnit.DAYS.between(last, today) == 1L
        return if (playedYesterday) currentStreakDays + 1 else 1
    }

    private fun replace(game: GameProgress): List<GameProgress> =
        if (games.any { it.gameId == game.gameId }) games.map { if (it.gameId == game.gameId) game else it }
        else games + game

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val MAX_RECENT_PER_GAME = 30
    }
}
