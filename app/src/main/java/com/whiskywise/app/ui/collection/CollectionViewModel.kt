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

    /** True only when loading an additional page (not the first load / filter change). */
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Total count as reported by the server for the current filter set. */
    private val _total = MutableLiveData(0)
    val total: LiveData<Int> = _total

    var currentQuery: String?  = null
    var currentStatus: String? = null
    var currentSort: String    = "score"
    var currentOrder: String   = "desc"
    var currentFlavor: String? = null
    var currentMinScore: Float? = null
    var currentMaxPrice: Float? = null

    /** When true, retired bottles are included. Matches web default (show all). */
    var showRetired: Boolean = true

    private val pageSize = 50
    private var currentOffset = 0
    private var allLoaded = false

    /** Full load — resets offset and replaces the list. Called on filter/sort changes. */
    fun load() {
        currentOffset = 0
        allLoaded     = false
        _isLoading.value = true
        viewModelScope.launch {
            repo.getCollection(
                query    = currentQuery,
                flavor   = currentFlavor,
                minScore = currentMinScore,
                maxPrice = currentMaxPrice,
                status   = currentStatus,
                sort     = currentSort,
                order    = currentOrder,
                retired  = if (showRetired) null else "no",
                limit    = pageSize,
                offset   = 0,
            ).fold(
                onSuccess = { resp ->
                    _whiskies.value = resp.data
                    _total.value    = resp.total
                    _error.value    = null
                    currentOffset   = resp.data.size
                    allLoaded       = currentOffset >= resp.total
                },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /** Append the next page. No-op if already loading or everything is fetched. */
    fun loadMore() {
        if (_isLoading.value == true || _isLoadingMore.value == true || allLoaded) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            repo.getCollection(
                query    = currentQuery,
                flavor   = currentFlavor,
                minScore = currentMinScore,
                maxPrice = currentMaxPrice,
                status   = currentStatus,
                sort     = currentSort,
                order    = currentOrder,
                retired  = if (showRetired) null else "no",
                limit    = pageSize,
                offset   = currentOffset,
            ).fold(
                onSuccess = { resp ->
                    val updated = (_whiskies.value ?: emptyList()) + resp.data
                    _whiskies.value = updated
                    _total.value    = resp.total
                    _error.value    = null
                    currentOffset   = updated.size
                    allLoaded       = currentOffset >= resp.total
                },
                onFailure = { _error.value = it.message },
            )
            _isLoadingMore.value = false
        }
    }

    /** True when more pages are available to load. */
    fun hasMore(): Boolean = !allLoaded
}
