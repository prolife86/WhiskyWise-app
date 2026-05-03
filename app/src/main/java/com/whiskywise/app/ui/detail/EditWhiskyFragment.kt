package com.whiskywise.app.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.databinding.FragmentEditWhiskyBinding
import com.whiskywise.app.model.WhiskyRequest

class EditWhiskyFragment : Fragment() {

    private var _binding: FragmentEditWhiskyBinding? = null
    private val binding get() = _binding!!
    private val vm: EditWhiskyViewModel by viewModels()

    private var editingId: Int = -1   // -1 = new whisky

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditWhiskyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editingId = arguments?.getInt("whiskyId") ?: -1

        if (editingId > 0) {
            vm.load(editingId)
            vm.whisky.observe(viewLifecycleOwner) { w ->
                if (w == null) return@observe
                binding.etName.setText(w.name)
                binding.etDistillery.setText(w.distillery)
                binding.etRegion.setText(w.region)
                binding.etAge.setText(w.age)
                binding.etAbv.setText(w.abv?.toString())
                binding.etScore.setText(w.score?.toString())
                binding.etPrice.setText(w.price?.toString())
                binding.etStore.setText(w.store)
                binding.etNose.setText(w.nose)
                binding.etPalate.setText(w.palate)
                binding.etFinish.setText(w.finish)
                binding.etNotes.setText(w.notes)
                // Status spinner
                val statuses = listOf("stashed", "open", "retired")
                binding.spinnerStatus.setSelection(statuses.indexOf(w.status ?: "stashed").coerceAtLeast(0))
            }
        }

        vm.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
        }
        vm.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) { binding.etName.error = "Required"; return }

        val statuses = listOf("stashed", "open", "retired")
        val request = WhiskyRequest(
            name        = name,
            distillery  = binding.etDistillery.text.toString().trim().ifBlank { null },
            region      = binding.etRegion.text.toString().trim().ifBlank { null },
            age         = binding.etAge.text.toString().trim().ifBlank { null },
            abv         = binding.etAbv.text.toString().toDoubleOrNull(),
            score       = binding.etScore.text.toString().toDoubleOrNull(),
            price       = binding.etPrice.text.toString().toDoubleOrNull(),
            store       = binding.etStore.text.toString().trim().ifBlank { null },
            nose        = binding.etNose.text.toString().trim().ifBlank { null },
            palate      = binding.etPalate.text.toString().trim().ifBlank { null },
            finish      = binding.etFinish.text.toString().trim().ifBlank { null },
            notes       = binding.etNotes.text.toString().trim().ifBlank { null },
            status      = statuses[binding.spinnerStatus.selectedItemPosition],
        )

        if (editingId > 0) vm.update(editingId, request)
        else vm.create(request)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
