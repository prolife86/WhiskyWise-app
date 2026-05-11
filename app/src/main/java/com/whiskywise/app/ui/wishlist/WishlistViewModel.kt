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

    /** Holds the single wishlist item being edited in EditWishlistFragment. */
    private val _editItem  = MutableLiveData<Whisky?>()
    val editItem: LiveData<Whisky?> = _editItem

    /** Fires true when an update save completes successfully. */
    private val _editSaved = MutableLiveData(false)
    val editSaved: LiveData<Boolean> = _editSaved

    var currentSort: String = "distillery"
    var currentOrder: String = "asc"

    fun load() {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getWishlist(sort = currentSort, order = currentOrder).fold(
                onSuccess = { _items.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun add(request: WhiskyRequest) {
        viewModelScope.launch {
            repo.createWishlistItem(request).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { _error.value = it.message },
            )
        }
    }

    /** Load a single wishlist item for the edit screen. */
    fun loadItem(id: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getWhisky(id).fold(
                onSuccess = { _editItem.value = it },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /** Save edits to an existing wishlist item. */
    fun update(id: Int, request: WhiskyRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.updateWishlistItem(id, request).fold(
                onSuccess = { _editSaved.value = true },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun clearEditSaved() { _editSaved.value = false }

    /** Delete a wishlist item. Calls [onDone] with true on success, false on failure. */
    fun delete(id: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.deleteWhisky(id).isSuccess
            onDone(ok)
        }
    }

    /** Clear the error after it has been shown to the user. */
    fun clearError() { _error.value = null }
}
