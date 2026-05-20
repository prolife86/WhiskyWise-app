package com.whiskywise.app.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Stats
import com.whiskywise.app.util.TokenStore
import kotlinx.coroutines.launch

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo      = WhiskyWiseRepository()
    private val tokenStore = TokenStore(app)

    private val _stats = MutableLiveData<Stats?>()
    val stats: LiveData<Stats?> = _stats

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        viewModelScope.launch {
            repo.getStats().fold(
                onSuccess = { stats ->
                    // Persist the server's currency symbol so all screens can use it
                    if (stats.currencySymbol.isNotBlank()) {
                        tokenStore.saveCurrencySymbol(stats.currencySymbol)
                    }
                    _stats.value = stats
                },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun clearError() { _error.value = null }
}
