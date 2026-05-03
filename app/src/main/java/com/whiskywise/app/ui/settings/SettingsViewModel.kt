package com.whiskywise.app.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.TokenListItem
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _tokens = MutableLiveData<List<TokenListItem>>()
    val tokens: LiveData<List<TokenListItem>> = _tokens

    private val _error  = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadTokens() {
        viewModelScope.launch {
            repo.listTokens().fold(
                onSuccess = { _tokens.value = it },
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
}
