package com.dolemes.braingames.game

import com.dolemes.braingames.core.CatalogEntry
import com.dolemes.braingames.games.flanker.FlankerGame

/** Todos os minijogos do app. Para incluir um jogo novo, acrescente-o aqui. */
object GameCatalog {
    val all: List<MiniGame> = listOf(
        FlankerGame,
    )

    fun find(id: String): MiniGame? = all.firstOrNull { it.definition.id == id }

    fun entries(): List<CatalogEntry> = all.map { CatalogEntry(it.definition.id, it.definition.domain) }
}
