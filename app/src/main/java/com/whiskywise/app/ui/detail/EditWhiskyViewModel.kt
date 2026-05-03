package com.whiskywise.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import com.whiskywise.app.model.WhiskyRequest
import kotlinx.coroutines.launch

class EditWhiskyViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _whisky    = MutableLiveData<Whisky?>()
    val whisky: LiveData<Whisky?> = _whisky

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saved     = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    fun load(id: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getWhisky(id).fold(
                onSuccess = { _whisky.value = it },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun create(request: WhiskyRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.createWhisky(request).fold(
                onSuccess = { _saved.value = true },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun update(id: Int, request: WhiskyRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.updateWhisky(id, request).fold(
                onSuccess = { _saved.value = true },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }
}
