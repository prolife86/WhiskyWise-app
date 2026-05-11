package com.whiskywise.app.ui.collection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import kotlinx.coroutines.launch

class CollectionViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _whiskies = MutableLiveData<List<Whisky>>()
    val whiskies: LiveData<List<Whisky>> = _whiskies

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    var currentQuery: String? = null
    var currentStatus: String? = null
    var currentSort: String = "score"
    var currentOrder: String = "desc"

    fun load() {
        _isLoading.value = true
        viewModelScope.launch {
            repo.getCollection(
                query  = currentQuery,
                status = currentStatus,
                sort   = currentSort,
                order  = currentOrder,
            ).fold(
                onSuccess = { resp ->
                    _whiskies.value = resp.data
                    _error.value = null
                },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }
}
