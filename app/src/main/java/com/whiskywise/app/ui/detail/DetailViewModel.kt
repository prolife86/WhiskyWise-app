package com.whiskywise.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _whisky    = MutableLiveData<Whisky?>()
    val whisky: LiveData<Whisky?> = _whisky

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load(id: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getWhisky(id).fold(
                onSuccess = { _whisky.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun delete(id: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(repo.deleteWhisky(id).isSuccess)
        }
    }
}
