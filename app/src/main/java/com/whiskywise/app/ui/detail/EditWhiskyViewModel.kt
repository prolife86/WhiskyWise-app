package com.whiskywise.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.model.Whisky
import com.whiskywise.app.model.WhiskyRequest
import kotlinx.coroutines.delay
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
     * Null = not yet saved / already consumed.
     * Call [clearSavedId] immediately after consuming to prevent re-delivery on rotation.
     */
    private val _savedId = MutableLiveData<Int?>()
    val savedId: LiveData<Int?> = _savedId

    /**
     * Emits the slot name after a server-side photo rotation succeeds, so the
     * fragment knows to bypass Glide's disk cache when the whisky observer fires.
     * Null = nothing pending. Call [clearRotatedSlot] after consuming.
     */
    private val _rotatedSlot = MutableLiveData<String?>()
    val rotatedSlot: LiveData<String?> = _rotatedSlot

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
     * Rotate a server-side photo 90° clockwise.
     * On success, emits [rotatedSlot] so the fragment bypasses Glide's disk
     * cache, then reloads the whisky. All loading state and errors are owned
     * by the ViewModel so they survive screen rotation correctly.
     */
    fun rotatePhoto(whiskyId: Int, slot: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.rotatePhoto(whiskyId, slot).fold(
                onSuccess = {
                    _rotatedSlot.value = slot
                    load(whiskyId)   // load() manages _isLoading from here
                },
                onFailure = {
                    _error.value = "Rotate failed: ${it.message}"
                    _isLoading.value = false
                },
            )
        }
    }

    fun clearRotatedSlot() { _rotatedSlot.value = null }

    /**
     * Consume [savedId] after handling it so that screen rotation
     * does not re-trigger photo uploads and back-stack navigation.
     */
    fun clearSavedId() { _savedId.value = null }

    /** Clear the error after it has been shown to the user. */
    fun clearError() { _error.value = null }

    /**
     * Upload new photos and delete removed ones for [whiskyId].
     * Errors are posted to [error] LiveData so the fragment's Snackbar observer
     * shows them before navigation. [onDone] is always called after all
     * operations finish — after a short delay on failure so the user can read
     * the error message before the screen pops.
     */
    fun processPhotos(
        whiskyId: Int,
        uploads: Map<String, File>,
        deletes: Set<String>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            var anyError = false
            for (slot in deletes) {
                repo.deletePhoto(whiskyId, slot)
                    .onFailure {
                        _error.value = "Could not remove $slot photo: ${it.message}"
                        anyError = true
                    }
            }
            for ((slot, file) in uploads) {
                repo.uploadPhoto(whiskyId, slot, file)
                    .onFailure {
                        _error.value = "Could not upload $slot photo: ${it.message}"
                        anyError = true
                    }
            }
            // Give the Snackbar time to show before navigating away on error.
            if (anyError) delay(2_000)
            onDone()
        }
    }
}
