package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val coins: Long = 0L,
    val coinsPerTap: Int = 1,
    val maxEnergy: Int = 500,
    val currentEnergy: Int = 500,
    val energyRecoveryRate: Int = 1, // Energy points tick up per second
    val passiveIncome: Int = 0,      // Coins per second passive drone
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val level: Int = 1
)
