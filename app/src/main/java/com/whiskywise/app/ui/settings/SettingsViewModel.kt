package com.whiskywise.app.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.ApiSession
import com.whiskywise.app.model.TokenListItem
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _tokens   = MutableLiveData<List<TokenListItem>>()
    val tokens: LiveData<List<TokenListItem>> = _tokens

    private val _sessions = MutableLiveData<List<ApiSession>>()
    val sessions: LiveData<List<ApiSession>> = _sessions

    private val _error    = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadAll() {
        loadTokens()
        loadSessions()
    }

    fun loadTokens() {
        viewModelScope.launch {
            repo.listTokens().fold(
                onSuccess = { _tokens.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            repo.listSessions().fold(
                onSuccess = { _sessions.value = it },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun revokeToken(id: Int) {
        viewModelScope.launch {
            repo.revokeToken(id).fold(
                onSuccess = { loadTokens() },
                onFailure = { _error.value = it.message },
            )
        }
    }

    fun revokeSession(id: Int) {
        viewModelScope.launch {
            repo.revokeSession(id).fold(
                onSuccess = { loadSessions() },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Clear the error after it has been shown to the user. */
    fun clearError() { _error.value = null }
}
