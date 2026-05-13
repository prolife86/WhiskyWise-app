package com.whiskywise.app.ui.collection

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentCollectionBinding
import com.whiskywise.app.ui.detail.BarcodeScanActivity
import com.whiskywise.app.util.TokenStore

class CollectionFragment : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val vm: CollectionViewModel by viewModels()
    private lateinit var adapter: WhiskyAdapter

    // Parallel arrays — index maps label → (sort, order) pair.
    private val sortKeys = listOf(
        "name"       to "asc",
        "name"       to "desc",
        "distillery" to "asc",
        "distillery" to "desc",
        "price"      to "asc",
        "price"      to "desc",
        "score"      to "asc",
        "score"      to "desc",
        "updated"    to "asc",
        "updated"    to "desc",
    )

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val barcode = result.data
                    ?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE)
                    ?: return@registerForActivityResult
                binding.searchView.setQuery(barcode, true)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = WhiskyAdapter { whisky ->
            findNavController().navigate(
                R.id.action_collection_to_detail,
                bundleOf("whiskyId" to whisky.id),
            )
        }

        val store = TokenStore(requireContext())
        adapter.setCredentials(store.getServerUrl() ?: "", store.getToken() ?: "")

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { vm.load() }

        vm.whiskies.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.isLoading.observe(viewLifecycleOwner) { binding.swipeRefresh.isRefreshing = it }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) binding.errorBanner.apply {
                text = err; visibility = View.VISIBLE
            } else binding.errorBanner.visibility = View.GONE
        }

        // Status filter chips
        val chipMap = listOf(
            null       to binding.chipAll,
            "open"     to binding.chipOpen,
            "stashed"  to binding.chipStashed,
            "finished" to binding.chipFinished,
        )
        chipMap.forEach { (status, chip) ->
            chip.setOnClickListener {
                vm.currentStatus = status
                vm.load()
                chipMap.forEach { (_, c) -> c.isChecked = (c == chip) }
            }
        }
        chipMap.forEach { (status, chip) -> chip.isChecked = (status == vm.currentStatus) }

        // Retired checkbox
        binding.checkRetired.isChecked = vm.showRetired
        binding.checkRetired.setOnCheckedChangeListener { _, checked ->
            vm.showRetired = checked
            vm.load()
        }

        // Sort spinner
        val sortLabels = resources.getStringArray(R.array.sort_labels)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        // Restore selection to match current VM state (default: score ↓ = index 7)
        val defaultIndex = sortKeys.indexOfFirst { it.first == vm.currentSort && it.second == vm.currentOrder }
        binding.spinnerSort.setSelection(if (defaultIndex >= 0) defaultIndex else 7, false)

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

        // Search
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean { vm.currentQuery = q; vm.load(); return true }
            override fun onQueryTextChange(q: String?): Boolean { if (q.isNullOrBlank()) { vm.currentQuery = null; vm.load() }; return true }
        })

        binding.btnBarcodeSearch.setOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), BarcodeScanActivity::class.java))
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_collection_to_edit, bundleOf("whiskyId" to -1))
        }

        vm.load()
    }

    override fun onResume() {
        super.onResume()
        vm.load()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
