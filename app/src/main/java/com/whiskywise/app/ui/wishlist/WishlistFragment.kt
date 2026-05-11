package com.whiskywise.app.ui.wishlist

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentWishlistBinding
import com.whiskywise.app.model.WhiskyRequest
import com.whiskywise.app.ui.collection.WhiskyAdapter
import com.whiskywise.app.ui.detail.BarcodeScanActivity
import com.whiskywise.app.util.TokenStore

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val vm: WishlistViewModel by viewModels()
    private lateinit var adapter: WhiskyAdapter

    // Parallel arrays — index maps label → (sort, order) pair.
    private val sortKeys = listOf(
        "distillery" to "asc",
        "distillery" to "desc",
        "name"       to "asc",
        "name"       to "desc",
        "price"      to "asc",
        "price"      to "desc",
        "score"      to "asc",
        "score"      to "desc",
    )

    private var dialogBarcodeField: TextInputEditText? = null

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val value = result.data
                    ?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE)
                    ?: return@registerForActivityResult
                dialogBarcodeField?.setText(value)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = WhiskyAdapter { whisky ->
            findNavController().navigate(
                R.id.action_wishlist_to_detail,
                bundleOf("whiskyId" to whisky.id),
            )
        }

        val store = TokenStore(requireContext())
        adapter.setCredentials(store.getServerUrl() ?: "", store.getToken() ?: "")
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { vm.load() }

        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.isLoading.observe(viewLifecycleOwner) { binding.swipeRefresh.isRefreshing = it }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        // Sort spinner
        val sortLabels = resources.getStringArray(R.array.sort_labels)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        // Restore selection to match current VM state (default: distillery ↑ = index 0)
        val defaultIndex = sortKeys.indexOfFirst { it.first == vm.currentSort && it.second == vm.currentOrder }
        binding.spinnerSort.setSelection(if (defaultIndex >= 0) defaultIndex else 0, false)

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val (sort, order) = sortKeys[pos]
                if (sort != vm.currentSort || order != vm.currentOrder) {
                    vm.currentSort = sort
                    vm.currentOrder = order
                    vm.load()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.fab.setOnClickListener { showAddDialog() }

        vm.load()
    }

    override fun onResume() {
        super.onResume()
        vm.load()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wishlist, null)
        val etName       = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etDistillery = dialogView.findViewById<TextInputEditText>(R.id.etDistillery)
        val etRegion     = dialogView.findViewById<TextInputEditText>(R.id.etRegion)
        val etPrice      = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etStore      = dialogView.findViewById<TextInputEditText>(R.id.etStore)
        val etBarcode    = dialogView.findViewById<TextInputEditText>(R.id.etBarcode)
        val btnScan      = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnScanBarcode)

        dialogBarcodeField = etBarcode
        btnScan.setOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), BarcodeScanActivity::class.java))
        }
        val etNotes = dialogView.findViewById<TextInputEditText>(R.id.etNotes)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add to wishlist")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isBlank()) {
                    Snackbar.make(binding.root, "Name is required", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                vm.add(WhiskyRequest(
                    name          = name,
                    distillery    = etDistillery.text.toString().trim().ifBlank { null },
                    region        = etRegion.text.toString().trim().ifBlank { null },
                    price         = etPrice.text.toString().toDoubleOrNull(),
                    store         = etStore.text.toString().trim().ifBlank { null },
                    barcode       = etBarcode.text.toString().trim().ifBlank { null },
                    wishlistNotes = etNotes.text.toString().trim().ifBlank { null },
                ))
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { dialogBarcodeField = null }
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
