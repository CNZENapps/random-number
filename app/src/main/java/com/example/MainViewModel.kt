package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("randomizer_prefs", Context.MODE_PRIVATE)

    private val _minRange = MutableStateFlow(sharedPrefs.getInt("min_range", 1))
    val minRange: StateFlow<Int> = _minRange.asStateFlow()

    private val _maxRange = MutableStateFlow(sharedPrefs.getInt("max_range", 100))
    val maxRange: StateFlow<Int> = _maxRange.asStateFlow()

    private val _allowDuplicates = MutableStateFlow(sharedPrefs.getBoolean("allow_duplicates", true))
    val allowDuplicates: StateFlow<Boolean> = _allowDuplicates.asStateFlow()

    private val _currentNumber = MutableStateFlow<Int?>(null)
    val currentNumber: StateFlow<Int?> = _currentNumber.asStateFlow()

    private val _isRolling = MutableStateFlow(false)
    val isRolling: StateFlow<Boolean> = _isRolling.asStateFlow()

    private val _history = MutableStateFlow<List<Int>>(emptyList())
    val history: StateFlow<List<Int>> = _history.asStateFlow()

    fun updateRange(min: Int, max: Int) {
        if (min <= max) {
            _minRange.value = min
            _maxRange.value = max
            sharedPrefs.edit().putInt("min_range", min).putInt("max_range", max).apply()
        }
    }

    fun setAllowDuplicates(allow: Boolean) {
        _allowDuplicates.value = allow
        sharedPrefs.edit().putBoolean("allow_duplicates", allow).apply()
    }

    fun roll() {
        if (_isRolling.value) return

        viewModelScope.launch {
            _isRolling.value = true

            val min = _minRange.value
            val max = _maxRange.value
            val prevRoll = _currentNumber.value

            if (max > min) {
                // Rapid count animation (speeds -> slows)
                val cycleCount = 10
                for (i in 1..cycleCount) {
                    _currentNumber.value = Random.nextInt(min, max + 1)
                    delay(25L + (i * 10L))
                }

                // Final true roll
                var finalRoll = Random.nextInt(min, max + 1)
                
                // If duplicates are forbidden sequentially and there's a previous roll to compare
                if (!_allowDuplicates.value && prevRoll != null && (max - min) >= 1) {
                    var attempts = 0
                    while (finalRoll == prevRoll && attempts < 20) {
                        finalRoll = Random.nextInt(min, max + 1)
                        attempts++
                    }
                }

                _currentNumber.value = finalRoll
                _history.value = (listOf(finalRoll) + _history.value).take(50)
            } else {
                _currentNumber.value = min
                _history.value = (listOf(min) + _history.value).take(50)
            }

            _isRolling.value = false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
