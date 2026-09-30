package com.dolemes.braingames

import com.dolemes.braingames.game.GameCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras que valem para todo o catálogo: pegam erros ao incluir um jogo novo. */
class GameCatalogTest {

    @Test
    fun ids_areUniqueAndKebabCase() {
        val ids = GameCatalog.all.map { it.definition.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("id inválido: $it", it.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) }
    }

    @Test
    fun definitions_haveSaneRoundSettings() {
        GameCatalog.all.forEach { game ->
            val d = game.definition
            assertTrue("${d.id}: rodada entre 30 e 180 s", d.roundSeconds in 30.0..180.0)
            assertTrue("${d.id}: cota sem cronômetro >= 5", d.untimedTrials >= 5)
        }
    }

    @Test
    fun find_returnsGameById() {
        GameCatalog.all.forEach { assertEquals(it, GameCatalog.find(it.definition.id)) }
    }
}
