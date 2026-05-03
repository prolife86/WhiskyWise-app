package com.whiskywise.app.ui.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import com.whiskywise.app.model.WhiskyRequest
import kotlinx.coroutines.launch

class WishlistViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _items     = MutableLiveData<List<Whisky>>()
    val items: LiveData<List<Whisky>> = _items

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getWishlist().fold(
                onSuccess = { _items.value = it },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun add(request: WhiskyRequest) {
        viewModelScope.launch {
            repo.createWishlistItem(request).fold(
                onSuccess = { load() },
                onFailure = { _error.value = it.message },
            )
        }
    }
}
