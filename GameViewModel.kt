package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.GameStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    
    private val _gameStats = MutableStateFlow(GameStats())
    val gameStats: StateFlow<GameStats> = _gameStats.asStateFlow()

    // Floating +Income text visual offsets spawned on taps
    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts: StateFlow<List<FloatingText>> = _floatingTexts.asStateFlow()

    private var gameTickJob: Job? = null

    init {
        val database = AppDatabase.getInstance(application)
        repository = GameRepository(database.dao)
        
        // Load stats reactively from database
        viewModelScope.launch {
            repository.statsFlow.collect { dbStats ->
                if (dbStats != null) {
                    _gameStats.value = dbStats
                } else {
                    // Seed initial row
                    repository.saveStats(GameStats())
                }
            }
        }

        // Start energetic ticking
        startGameTickers()
    }

    private fun startGameTickers() {
        gameTickJob?.cancel()
        gameTickJob = viewModelScope.launch {
            // Allow initial DB values mapping
            delay(800)
            
            // Offline passive mining calculation
            val stats = _gameStats.value
            val timeDiffSec = (System.currentTimeMillis() - stats.lastActiveTimestamp) / 1000
            if (timeDiffSec > 5 && stats.passiveIncome > 0) {
                // Max idle mining accumulation set to 3 hours (10800 seconds)
                val activeOfflineSec = Math.min(timeDiffSec, 10800)
                val offlineGains = stats.passiveIncome * activeOfflineSec
                _gameStats.update { 
                    it.copy(
                        coins = it.coins + offlineGains,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                }
                saveStatsToDb()
            }

            // High frequency secondary state loops (1Hz updates)
            while (true) {
                delay(1000L)
                _gameStats.update { current ->
                    val nextEnergy = Math.min(current.maxEnergy, current.currentEnergy + current.energyRecoveryRate)
                    val nextCoins = current.coins + current.passiveIncome
                    
                    current.copy(
                        currentEnergy = nextEnergy,
                        coins = nextCoins,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                }
                saveStatsToDb()
            }
        }
    }

    private fun saveStatsToDb() {
        viewModelScope.launch {
            repository.saveStats(_gameStats.value)
        }
    }

    fun handleTap(x: Float, y: Float) {
        val current = _gameStats.value
        val tapPower = current.coinsPerTap
        
        if (current.currentEnergy >= tapPower) {
            // Fire short low-latency high-pitch synthesised coin sound
            SoundManager.playCoinSound()

            // Update local game statistics
            _gameStats.update { 
                val newCoins = it.coins + tapPower
                it.copy(
                    coins = newCoins,
                    currentEnergy = it.currentEnergy - tapPower,
                    level = calculateLevel(newCoins)
                )
            }
            saveStatsToDb()

            // Spawn visual "+Power" label floating on screen
            spawnFloatingText(tapPower, x, y)
        }
    }

    private fun calculateLevel(totalCoins: Long): Int {
        return when {
            totalCoins < 1000L -> 1
            totalCoins < 10000L -> 2
            totalCoins < 50000L -> 3
            totalCoins < 250000L -> 4
            totalCoins < 1000000L -> 5
            totalCoins < 5000000L -> 6
            totalCoins < 20000000L -> 7
            else -> 8
        }
    }

    private fun spawnFloatingText(amount: Int, x: Float, y: Float) {
        val id = System.currentTimeMillis() + (0..10000).random()
        val textItem = FloatingText(id = id, text = "+$amount", x = x, y = y)
        
        _floatingTexts.update { it + textItem }
        
        // Let it live for 750 milliseconds before deleting
        viewModelScope.launch {
            delay(750)
            _floatingTexts.update { current -> current.filter { it.id != id } }
        }
    }

    // --- Price Formula Controllers ---

    fun getMultiTapUpgradeCost(): Long {
        val currentPower = _gameStats.value.coinsPerTap
        return 80L * currentPower * currentPower + 20L
    }

    fun getEnergyUpgradeCost(): Long {
        val currentMax = _gameStats.value.maxEnergy
        val tier = (currentMax - 500) / 100 + 1
        return 120L * tier * tier + 80L
    }

    fun getDroneUpgradeCost(): Long {
        val currentIncome = _gameStats.value.passiveIncome
        return 180L * (currentIncome + 1) * (currentIncome + 1) + 120L
    }

    fun getRechargeUpgradeCost(): Long {
        val currentRate = _gameStats.value.energyRecoveryRate
        return 400L * currentRate * currentRate + 200L
    }

    // --- Action Button Click Handlers ---

    fun buyMultiTap() {
        val cost = getMultiTapUpgradeCost()
        val current = _gameStats.value
        if (current.coins >= cost) {
            _gameStats.update { 
                it.copy(
                    coins = it.coins - cost,
                    coinsPerTap = it.coinsPerTap + 1
                )
            }
            saveStatsToDb()
        }
    }

    fun buyEnergyCapacity() {
        val cost = getEnergyUpgradeCost()
        val current = _gameStats.value
        if (current.coins >= cost) {
            _gameStats.update {
                it.copy(
                    coins = it.coins - cost,
                    maxEnergy = it.maxEnergy + 100,
                    currentEnergy = it.currentEnergy + 100
                )
            }
            saveStatsToDb()
        }
    }

    fun buyDrone() {
        val cost = getDroneUpgradeCost()
        val current = _gameStats.value
        if (current.coins >= cost) {
            _gameStats.update {
                it.copy(
                    coins = it.coins - cost,
                    passiveIncome = it.passiveIncome + 1
                )
            }
            saveStatsToDb()
        }
    }

    fun buyRechargeBoost() {
        val cost = getRechargeUpgradeCost()
        val current = _gameStats.value
        if (current.coins >= cost) {
            _gameStats.update {
                it.copy(
                    coins = it.coins - cost,
                    energyRecoveryRate = it.energyRecoveryRate + 1
                )
            }
            saveStatsToDb()
        }
    }

    fun resetGame() {
        _gameStats.value = GameStats()
        saveStatsToDb()
    }
}

data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float
)
