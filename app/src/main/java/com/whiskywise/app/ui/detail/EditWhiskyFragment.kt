package com.whiskywise.app.ui.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentEditWhiskyBinding
import com.whiskywise.app.model.WhiskyRequest
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.loadWhiskyPhoto
import java.io.File
import java.io.FileOutputStream

class EditWhiskyFragment : Fragment() {

    private var _binding: FragmentEditWhiskyBinding? = null
    private val binding get() = _binding!!
    private val vm: EditWhiskyViewModel by viewModels()

    private var editingId: Int = -1
    private var existingFlavorProfile: String? = null
    private var pendingSlot: String? = null
    private val uploadQueue = mutableMapOf<String, File>()
    private val deleteQueue = mutableSetOf<String>()

    // Temp file URI for camera capture; set before launching TakePicture.
    private var cameraOutputUri: Uri? = null

    // Slot waiting for camera permission grant before the camera is launched.
    private var pendingCameraSlot: String? = null

    // ── Camera permission request ─────────────────────────────────────────────
    // Requested here because TakePicture does not ask for CAMERA permission itself.
    // BarcodeScanActivity handles its own permission; this is only for photo capture.

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission just granted — proceed with the slot that triggered the request.
                pendingCameraSlot?.let { launchCamera(it) }
            } else {
                Snackbar.make(binding.root, "Camera permission is required to take photos", Snackbar.LENGTH_LONG).show()
            }
            pendingCameraSlot = null
        }

    // ── Gallery picker ────────────────────────────────────────────────────────

    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            val slot = pendingSlot ?: return@registerForActivityResult
            pendingSlot = null
            handlePickedUri(uri, slot)
        }

    // ── Camera capture ────────────────────────────────────────────────────────

    private val cameraPicker =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (!success) return@registerForActivityResult
            val uri  = cameraOutputUri ?: return@registerForActivityResult
            val slot = pendingSlot      ?: return@registerForActivityResult
            cameraOutputUri = null
            pendingSlot     = null
            handlePickedUri(uri, slot)
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

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditWhiskyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editingId = arguments?.getInt("whiskyId", -1) ?: -1

        wireSliders()
        wirePhotoButtons()

        binding.btnScanBarcode.setOnClickListener {
            barcodeLauncher.launch(
                Intent(requireContext(), BarcodeScanActivity::class.java)
            )
        }

        if (editingId > 0) {
            vm.load(editingId)
            vm.whisky.observe(viewLifecycleOwner) { w ->
                if (w == null) return@observe
                existingFlavorProfile = w.flavorProfile

                binding.etName.setText(w.name)
                binding.etDistillery.setText(w.distillery)
                binding.etRegion.setText(w.region)
                binding.etAge.setText(w.age)
                binding.etAbv.setText(w.abv?.toString())
                binding.etScore.setText(w.score?.toString())
                binding.etPrice.setText(w.price?.toString())
                binding.etStore.setText(w.store)
                binding.etBarcode.setText(w.barcode)
                binding.etNose.setText(w.nose)
                binding.etPalate.setText(w.palate)
                binding.etFinish.setText(w.finish)
                binding.etNotes.setText(w.notes)

                val statuses = listOf("stashed", "open", "retired")
                binding.spinnerStatus.setSelection(
                    statuses.indexOf(w.status ?: "stashed").coerceAtLeast(0)
                )

                setSlider(binding.seekWoody,     binding.tvWoody,     w.radarWoody)
                setSlider(binding.seekSmoky,     binding.tvSmoky,     w.radarSmoky)
                setSlider(binding.seekCereal,    binding.tvCereal,    w.radarCereal)
                setSlider(binding.seekFloral,    binding.tvFloral,    w.radarFloral)
                setSlider(binding.seekFruity,    binding.tvFruity,    w.radarFruity)
                setSlider(binding.seekMedicinal, binding.tvMedicinal, w.radarMedicinal)
                setSlider(binding.seekFiery,     binding.tvFiery,     w.radarFiery)

                val ctx   = requireContext()
                val store = TokenStore(ctx)
                binding.ivPhotoFront.loadWhiskyPhoto(ctx, w.photoFront, store.getServerUrl() ?: "", store.getToken() ?: "")
                binding.ivPhotoBack.loadWhiskyPhoto(ctx,  w.photoBack,  store.getServerUrl() ?: "", store.getToken() ?: "")
                binding.ivPhotoCask.loadWhiskyPhoto(ctx,  w.photoCask,  store.getServerUrl() ?: "", store.getToken() ?: "")

                if (!w.photoFront.isNullOrBlank()) binding.btnDeleteFront.visibility = View.VISIBLE
                if (!w.photoBack.isNullOrBlank())  binding.btnDeleteBack.visibility  = View.VISIBLE
                if (!w.photoCask.isNullOrBlank())  binding.btnDeleteCask.visibility  = View.VISIBLE
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
        vm.savedId.observe(viewLifecycleOwner) { savedId ->
            if (savedId == null) return@observe
            vm.clearSavedId()
            vm.processPhotos(savedId, uploadQueue.toMap(), deleteQueue.toSet()) {
                findNavController().popBackStack()
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun wireSliders() {
        fun wire(seek: SeekBar, label: TextView) {
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { label.text = p.toString() }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        wire(binding.seekWoody,     binding.tvWoody)
        wire(binding.seekSmoky,     binding.tvSmoky)
        wire(binding.seekCereal,    binding.tvCereal)
        wire(binding.seekFloral,    binding.tvFloral)
        wire(binding.seekFruity,    binding.tvFruity)
        wire(binding.seekMedicinal, binding.tvMedicinal)
        wire(binding.seekFiery,     binding.tvFiery)
    }

    private fun wirePhotoButtons() {
        fun pick(slot: String) { showPhotoSourceDialog(slot) }
        fun remove(slot: String, iv: ImageView, btn: MaterialButton) {
            deleteQueue.add(slot)
            uploadQueue.remove(slot)
            iv.setImageResource(R.drawable.ic_whisky_placeholder)
            btn.visibility = View.GONE
        }
        binding.btnPickFront.setOnClickListener   { pick("front") }
        binding.btnPickBack.setOnClickListener    { pick("back") }
        binding.btnPickCask.setOnClickListener    { pick("cask") }
        binding.btnDeleteFront.setOnClickListener { remove("front", binding.ivPhotoFront, binding.btnDeleteFront) }
        binding.btnDeleteBack.setOnClickListener  { remove("back",  binding.ivPhotoBack,  binding.btnDeleteBack) }
        binding.btnDeleteCask.setOnClickListener  { remove("cask",  binding.ivPhotoCask,  binding.btnDeleteCask) }
    }

    private fun showPhotoSourceDialog(slot: String) {
        val options = arrayOf("📷  Take photo", "🖼️  Choose from gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraAndLaunch(slot)
                    1 -> launchGallery(slot)
                }
            }
            .show()
    }

    /**
     * Check CAMERA permission before launching the camera.
     * TakePicture does not request the permission itself — if the user hasn't
     * previously granted it (e.g. via the barcode scanner), the capture silently
     * fails or crashes. We request it here, then proceed in the permission callback.
     */
    private fun requestCameraAndLaunch(slot: String) {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                // Already granted — launch immediately.
                launchCamera(slot)
            }
            else -> {
                // Store the slot so the permission callback knows what to launch.
                pendingCameraSlot = slot
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera(slot: String) {
        pendingSlot = slot
        val tmp = File(requireContext().cacheDir, "camera_capture_${slot}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            tmp
        )
        cameraOutputUri = uri
        cameraPicker.launch(uri)
    }

    private fun launchGallery(slot: String) {
        pendingSlot = slot
        galleryPicker.launch("image/*")
    }

    private fun handlePickedUri(uri: Uri, slot: String) {
        val tmp = uriToTempFile(uri, slot) ?: return
        uploadQueue[slot] = tmp
        deleteQueue.remove(slot)
        previewFromUri(slot, uri)
    }

    private fun setSlider(seek: SeekBar, label: TextView, value: Int) {
        seek.progress = value.coerceIn(0, 5)
        label.text    = seek.progress.toString()
    }

    private fun previewFromUri(slot: String, uri: Uri) {
        val (iv, btn) = when (slot) {
            "front" -> Pair(binding.ivPhotoFront, binding.btnDeleteFront)
            "back"  -> Pair(binding.ivPhotoBack,  binding.btnDeleteBack)
            "cask"  -> Pair(binding.ivPhotoCask,  binding.btnDeleteCask)
            else    -> return
        }
        iv.setImageURI(uri)
        btn.visibility = View.VISIBLE
    }

    private fun uriToTempFile(uri: Uri, slot: String): File? = try {
        val tmp = File(requireContext().cacheDir, "photo_${slot}_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri)
            ?.use { input -> FileOutputStream(tmp).use { input.copyTo(it) } }
        tmp
    } catch (e: Exception) {
        Snackbar.make(binding.root, "Could not read image: ${e.message}", Snackbar.LENGTH_SHORT).show()
        null
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) { binding.etName.error = "Required"; return }

        val statuses = listOf("stashed", "open", "retired")
        val req = WhiskyRequest(
            name           = name,
            distillery     = binding.etDistillery.text.toString().trim().ifBlank { null },
            region         = binding.etRegion.text.toString().trim().ifBlank { null },
            age            = binding.etAge.text.toString().trim().ifBlank { null },
            abv            = binding.etAbv.text.toString().toDoubleOrNull(),
            score          = binding.etScore.text.toString().toDoubleOrNull(),
            price          = binding.etPrice.text.toString().toDoubleOrNull(),
            store          = binding.etStore.text.toString().trim().ifBlank { null },
            barcode        = binding.etBarcode.text.toString().trim().ifBlank { null },
            nose           = binding.etNose.text.toString().trim().ifBlank { null },
            palate         = binding.etPalate.text.toString().trim().ifBlank { null },
            finish         = binding.etFinish.text.toString().trim().ifBlank { null },
            notes          = binding.etNotes.text.toString().trim().ifBlank { null },
            status         = statuses[binding.spinnerStatus.selectedItemPosition],
            flavorProfile  = existingFlavorProfile,
            radarWoody     = binding.seekWoody.progress,
            radarSmoky     = binding.seekSmoky.progress,
            radarCereal    = binding.seekCereal.progress,
            radarFloral    = binding.seekFloral.progress,
            radarFruity    = binding.seekFruity.progress,
            radarMedicinal = binding.seekMedicinal.progress,
            radarFiery     = binding.seekFiery.progress,
        )

        if (editingId > 0) vm.update(editingId, req) else vm.create(req)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
