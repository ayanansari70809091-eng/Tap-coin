package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameStatsDao) {
    val statsFlow: Flow<GameStats?> = dao.getGameStatsFlow()

    suspend fun getStats(): GameStats? = dao.getGameStats()

    suspend fun saveStats(stats: GameStats) {
        dao.insertOrUpdate(stats)
    }
}
