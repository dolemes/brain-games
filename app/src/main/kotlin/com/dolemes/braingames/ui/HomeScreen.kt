package com.dolemes.braingames.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dolemes.braingames.AppContainer
import com.dolemes.braingames.R
import com.dolemes.braingames.core.CognitiveDomain
import com.dolemes.braingames.core.DailyWorkoutPlanner
import com.dolemes.braingames.core.PlayerProgress
import com.dolemes.braingames.game.GameCatalog
import com.dolemes.braingames.game.MiniGameDefinition
import com.dolemes.braingames.game.labelRes
import java.time.LocalDate

data class WorkoutItem(val definition: MiniGameDefinition, val done: Boolean)

/** Tudo o que a Home mostra. Separado da tela para testar e gerar capturas sem o app inteiro. */
data class HomeUiState(
    val workout: List<WorkoutItem>,
    val workoutComplete: Boolean,
    val streakDays: Int,
    val gamesByDomain: List<Pair<CognitiveDomain, List<MiniGameDefinition>>>,
) {
    companion object {
        fun from(progress: PlayerProgress, definitions: List<MiniGameDefinition>, today: LocalDate): HomeUiState {
            val byId = definitions.associateBy { it.id }
            val workout = progress.workout.takeIf { it.date == today.toString() }
            val items = workout?.gameIds.orEmpty().mapNotNull { id ->
                byId[id]?.let { WorkoutItem(it, id in workout!!.doneIds) }
            }
            // A sequência só vale se o jogador jogou hoje ou ontem.
            val streakAlive = progress.lastPlayedDate == today.toString() ||
                progress.lastPlayedDate == today.minusDays(1).toString()
            return HomeUiState(
                workout = items,
                workoutComplete = workout?.isComplete(today) == true,
                streakDays = if (streakAlive) progress.currentStreakDays else 0,
                gamesByDomain = definitions.groupBy { it.domain }.toList().sortedBy { it.first.ordinal },
            )
        }
    }
}

@Composable
fun HomeRoute(
    container: AppContainer,
    progress: PlayerProgress,
    onPlay: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    // Se o app ficar aberto depois da meia-noite, o treino do dia muda ao voltar para ele.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }
    LaunchedEffect(today) {
        container.progress.update { DailyWorkoutPlanner.ensureToday(it, GameCatalog.entries(), today) }
    }
    val state = remember(progress, today) {
        HomeUiState.from(progress, GameCatalog.all.map { it.definition }, today)
    }
    HomeScreen(state = state, onPlay = onPlay, onSettings = onSettings)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.settings))
                }
            }
        }
        if (state.streakDays > 0) {
            item {
                Text(
                    text = pluralStringResource(R.plurals.home_streak, state.streakDays, state.streakDays),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            Text(stringResource(R.string.home_workout_title), style = MaterialTheme.typography.titleLarge)
        }
        if (state.workoutComplete) {
            item { Text(stringResource(R.string.home_workout_done), style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.workout, key = { "w-" + it.definition.id }) { item ->
            GameCard(item.definition, done = item.done, onPlay = { onPlay(item.definition.id) })
        }
        item {
            Text(
                text = stringResource(R.string.home_all_games),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        state.gamesByDomain.forEach { (domain, games) ->
            item(key = "d-" + domain.name) {
                Text(
                    text = stringResource(domain.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(games, key = { "g-" + it.id }) { definition ->
                GameCard(definition, done = false, onPlay = { onPlay(definition.id) })
            }
        }
    }
}

@Composable
private fun GameCard(definition: MiniGameDefinition, done: Boolean, onPlay: () -> Unit) {
    Card(onClick = onPlay, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(definition.nameRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(definition.domain.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (done) "✓ " + stringResource(R.string.home_done) else stringResource(R.string.home_play),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
