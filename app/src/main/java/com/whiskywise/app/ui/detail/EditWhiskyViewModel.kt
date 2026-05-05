package com.whiskywise.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import com.whiskywise.app.model.WhiskyRequest
import kotlinx.coroutines.launch
import java.io.File

class EditWhiskyViewModel : ViewModel() {

    private val repo = WhiskyWiseRepository()

    private val _whisky    = MutableLiveData<Whisky?>()
    val whisky: LiveData<Whisky?> = _whisky

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Emits the saved whisky's ID once a create or update succeeds.
     * The fragment observes this to trigger photo uploads, then navigates back.
     * Null = not yet saved.
     */
    private val _savedId = MutableLiveData<Int?>()
    val savedId: LiveData<Int?> = _savedId

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
                onSuccess = { w -> _savedId.value = w.id },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    fun update(id: Int, request: WhiskyRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.updateWhisky(id, request).fold(
                onSuccess = { w -> _savedId.value = w.id },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /**
     * Upload new photos and delete removed ones for [whiskyId],
     * then call [onDone] on the main thread.
     */
    fun processPhotos(
        whiskyId: Int,
        uploads: Map<String, File>,
        deletes: Set<String>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            for (slot in deletes) {
                repo.deletePhoto(whiskyId, slot)
                    .onFailure { _error.value = "Could not remove $slot photo: ${it.message}" }
            }
            for ((slot, file) in uploads) {
                repo.uploadPhoto(whiskyId, slot, file)
                    .onFailure { _error.value = "Could not upload $slot photo: ${it.message}" }
            }
            onDone()
        }
    }
}
