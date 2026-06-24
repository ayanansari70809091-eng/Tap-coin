package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getGameStatsFlow(): Flow<GameStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    suspend fun getGameStats(): GameStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: GameStats)
}
