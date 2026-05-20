package com.whiskywise.app.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Stats
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _stats = MutableLiveData<Stats?>()
    val stats: LiveData<Stats?> = _stats

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        viewModelScope.launch {
            repo.getStats().fold(
                onSuccess = { _stats.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun clearError() { _error.value = null }
}
