package com.whiskywise.app.ui.wishlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.databinding.FragmentEditWishlistBinding
import com.whiskywise.app.model.WhiskyRequest
import com.whiskywise.app.ui.detail.BarcodeScanActivity
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.formatForEdit
import com.whiskywise.app.util.loadWhiskyPhoto
import java.io.File
import java.io.FileOutputStream

/**
 * Edit screen for wishlist items.
 * Mirrors the server's wishlist_form.html:
 * Name, Distillery, Region, Age, ABV, Price, Store, Barcode, Notes, Cover Photo.
 * The cover photo is stored as photo_front and becomes the Front Label photo
 * when the item is promoted to the collection.
 */
class EditWishlistFragment : Fragment() {

    private var _binding: FragmentEditWishlistBinding? = null
    private val binding get() = _binding!!
    private val vm: WishlistViewModel by viewModels()

    private var pendingCoverUri: Uri? = null      // queued for upload on save
    private var coverFileToUpload: File? = null   // local temp file
    private var editingId: Int = -1

    // ── Camera permission ─────────────────────────────────────────────────────
    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else Snackbar.make(binding.root, "Camera permission required", Snackbar.LENGTH_SHORT).show()
        }

    // ── Camera capture ────────────────────────────────────────────────────────
    private var cameraOutputUri: Uri? = null
    private val cameraPicker =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (!success) return@registerForActivityResult
            val uri = cameraOutputUri ?: return@registerForActivityResult
            handlePickedUri(uri)
        }

    // ── Gallery picker ────────────────────────────────────────────────────────
    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            handlePickedUri(uri)
        }

    // ── Barcode scanner ───────────────────────────────────────────────────────
    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val value = result.data
                    ?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE)
                    ?: return@registerForActivityResult
                binding.etBarcode.setText(value)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editingId = arguments?.getInt("whiskyId", -1) ?: -1

        val store     = TokenStore(requireContext())
        val serverUrl = store.getServerUrl() ?: ""
        val token     = store.getToken() ?: ""

        // Trash icon — only when editing an existing item
        if (editingId > 0) {
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_edit_wishlist, menu)
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId == R.id.action_delete) {
                        confirmDelete(editingId); return true
                    }
                    return false
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

            vm.loadItem(editingId)
            vm.editItem.observe(viewLifecycleOwner) { w ->
                if (w == null) return@observe
                binding.etName.setText(w.name)
                binding.etDistillery.setText(w.distillery)
                binding.etRegion.setText(w.region)
                binding.etAge.setText(w.age)
                binding.etAbv.setText(w.abv?.formatForEdit(1, store.getCurrencyCode()))
                binding.etPrice.setText(w.price?.formatForEdit(2, store.getCurrencyCode()))
                binding.etStore.setText(w.store)
                binding.etBarcode.setText(w.barcode)
                binding.etWishlistNotes.setText(w.wishlistNotes)
                // Load existing cover photo
                if (!w.photoFront.isNullOrBlank()) {
                    binding.ivPhotoCover.loadWhiskyPhoto(requireContext(), w.photoFront, serverUrl, token)
                }
            }
        }

        vm.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                vm.clearError()
            }
        }
        vm.editSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                vm.clearEditSaved()
                // Upload cover photo if one was picked
                val file = coverFileToUpload
                if (file != null && editingId > 0) {
                    lifecycleScope.launch {
                        WhiskyWiseRepository().uploadPhoto(editingId, "front", file)
                    }
                }
                findNavController().popBackStack()
            }
        }

        binding.btnSave.setOnClickListener { save(editingId) }
        binding.btnScanBarcode.setOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), BarcodeScanActivity::class.java))
        }
        binding.btnPickCover.setOnClickListener { showPhotoSourceDialog() }
        binding.ivPhotoCover.setOnClickListener { showPhotoSourceDialog() }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("📷  Take photo", "🖼️  Choose from gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Cover photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) launchCamera()
                        else cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                    1 -> galleryPicker.launch("image/*")
                }
            }.show()
    }

    private fun launchCamera() {
        val tmp = File(requireContext().cacheDir, "wishlist_cover_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", tmp
        )
        cameraOutputUri = uri
        cameraPicker.launch(uri)
    }

    private fun handlePickedUri(uri: Uri) {
        try {
            val tmp = File(requireContext().cacheDir, "wishlist_cover_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)
                ?.use { input -> FileOutputStream(tmp).use { input.copyTo(it) } }
            coverFileToUpload = tmp
            binding.ivPhotoCover.setImageURI(uri)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Could not read image", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun save(id: Int) {
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) { binding.etName.error = "Required"; return }

        val req = WhiskyRequest(
            name          = name,
            distillery    = binding.etDistillery.text.toString().trim().ifBlank { null },
            region        = binding.etRegion.text.toString().trim().ifBlank { null },
            age           = binding.etAge.text.toString().trim().ifBlank { null },
            abv           = binding.etAbv.text.toString().replace(',', '.').toDoubleOrNull(),
            price         = binding.etPrice.text.toString().toDoubleOrNull(),
            store         = binding.etStore.text.toString().trim().ifBlank { null },
            barcode       = binding.etBarcode.text.toString().trim().ifBlank { null },
            wishlistNotes = binding.etWishlistNotes.text.toString().trim().ifBlank { null },
        )
        vm.update(id, req)
    }

    private fun confirmDelete(id: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove from wishlist?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                vm.delete(id) { ok ->
                    if (ok) findNavController().popBackStack()
                    else Snackbar.make(binding.root, "Delete failed", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
