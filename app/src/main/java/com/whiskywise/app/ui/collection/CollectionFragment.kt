package com.whiskywise.app.ui.collection

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.R
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.databinding.FragmentCollectionBinding
import com.whiskywise.app.ui.detail.BarcodeScanActivity
import com.whiskywise.app.util.TokenStore

class CollectionFragment : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val vm: CollectionViewModel by viewModels()
    private lateinit var adapter: WhiskyAdapter

    // Index 0 = All (null), 1 = open, 2 = stashed, 3 = finished
    private val statusKeys = listOf(null, "open", "stashed", "finished")

    private val sortKeys = listOf(
        "name"        to "asc",
        "name"        to "desc",
        "distillery"  to "asc",
        "distillery"  to "desc",
        "price"       to "asc",
        "price"       to "desc",
        "score"       to "asc",
        "score"       to "desc",
        "updated"     to "asc",
        "updated"     to "desc",
        "last_tasted" to "asc",
        "last_tasted" to "desc",
    )

    // Index 0 = All (null), then matches flavor_filter_labels exactly
    private val flavorKeys = listOf(
        null,
        "floral", "fresh", "fruity", "malty", "medicinal", "oily",
        "peaty", "smoky", "spicy", "sweet", "vanilla", "vegetative",
        "woody", "mixed", "undefinable", "complicated",
    )

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val barcode = result.data
                    ?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE)
                    ?: return@registerForActivityResult
                handleScannedBarcode(barcode)
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
        adapter.setCredentials(store.getServerUrl() ?: "", store.getToken() ?: "", store.getCurrencyCode())

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { vm.load() }

        // ── Infinite scroll ───────────────────────────────────────────────────
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return // only trigger on downward scroll
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total       = layoutManager.itemCount
                // Trigger when within 8 items of the bottom
                if (total > 0 && lastVisible >= total - 8) {
                    vm.loadMore()
                }
            }
        })

        vm.whiskies.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        vm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            // Hide the "loading more" footer when a full reload starts
            if (loading) binding.loadingMoreBar.visibility = View.GONE
        }

        // Show a subtle footer spinner while loading the next page
        vm.isLoadingMore.observe(viewLifecycleOwner) { loading ->
            binding.loadingMoreBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) binding.errorBanner.apply {
                text = err; visibility = View.VISIBLE
            } else binding.errorBanner.visibility = View.GONE
        }

        // ── Status spinner ────────────────────────────────────────────────────
        val statusLabels = resources.getStringArray(R.array.status_labels)
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusLabels)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        val defaultStatusIndex = statusKeys.indexOf(vm.currentStatus).coerceAtLeast(0)
        binding.spinnerStatus.setSelection(defaultStatusIndex, false)

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val status = statusKeys[pos]
                if (status != vm.currentStatus) { vm.currentStatus = status; vm.load() }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ── Retired checkbox ──────────────────────────────────────────────────
        binding.checkRetired.isChecked = vm.showRetired
        binding.checkRetired.setOnCheckedChangeListener { _, checked ->
            vm.showRetired = checked; vm.load()
        }

        // ── Sort spinner ──────────────────────────────────────────────────────
        val sortLabels = resources.getStringArray(R.array.sort_labels)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        val defaultIndex = sortKeys.indexOfFirst { it.first == vm.currentSort && it.second == vm.currentOrder }
        binding.spinnerSort.setSelection(if (defaultIndex >= 0) defaultIndex else 7, false)

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val (sort, order) = sortKeys[pos]
                if (sort != vm.currentSort || order != vm.currentOrder) {
                    vm.currentSort = sort; vm.currentOrder = order; vm.load()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ── Flavour spinner ───────────────────────────────────────────────────
        val flavorLabels = resources.getStringArray(R.array.flavor_filter_labels)
        val flavorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, flavorLabels)
        flavorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFlavor.adapter = flavorAdapter

        val defaultFlavorIndex = flavorKeys.indexOf(vm.currentFlavor).coerceAtLeast(0)
        binding.spinnerFlavor.setSelection(defaultFlavorIndex, false)

        binding.spinnerFlavor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val flavor = flavorKeys[pos]
                if (flavor != vm.currentFlavor) { vm.currentFlavor = flavor; vm.load() }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // ── Min score input ───────────────────────────────────────────────────
        if (vm.currentMinScore != null) binding.etMinScore.setText(vm.currentMinScore.toString())
        binding.etMinScore.doAfterTextChanged { text ->
            val v = text?.toString()?.toFloatOrNull()
            if (v != vm.currentMinScore) { vm.currentMinScore = v; vm.load() }
        }

        // ── Max price input ───────────────────────────────────────────────────
        binding.etMaxPrice.hint = "Max ${TokenStore(requireContext()).getCurrencySymbol()}"
        if (vm.currentMaxPrice != null) binding.etMaxPrice.setText(vm.currentMaxPrice.toString())
        binding.etMaxPrice.doAfterTextChanged { text ->
            val v = text?.toString()?.toFloatOrNull()
            if (v != vm.currentMaxPrice) { vm.currentMaxPrice = v; vm.load() }
        }

        // ── Search ────────────────────────────────────────────────────────────
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean { vm.currentQuery = q; vm.load(); return true }
            override fun onQueryTextChange(q: String?): Boolean {
                if (q.isNullOrBlank()) { vm.currentQuery = null; vm.load() }; return true
            }
        })

        binding.btnBarcodeSearch.setOnClickListener {
            barcodeLauncher.launch(Intent(requireContext(), BarcodeScanActivity::class.java))
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_collection_to_edit, bundleOf("whiskyId" to -1))
        }

        vm.load()
    }

    /**
     * After a barcode scan on the collection screen:
     * 1. Look it up via the API.
     * 2. Found     → navigate straight to that whisky's detail page.
     * 3. Not found → offer to add a new whisky with the barcode pre-filled.
     * 4. Error     → fall back to a regular text search.
     */
    private fun handleScannedBarcode(barcode: String) {
        val repo = WhiskyWiseRepository()
        lifecycleScope.launch {
            repo.barcodeLookup(barcode).fold(
                onSuccess = { result ->
                    if (result.found && result.id != null) {
                        // Already in collection — go straight to it
                        findNavController().navigate(
                            R.id.action_collection_to_detail,
                            bundleOf("whiskyId" to result.id),
                        )
                    } else {
                        // Not found — offer to add
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Barcode not found")
                            .setMessage("This barcode isn't in your collection yet.\n\nAdd a new whisky with it pre-filled?")
                            .setPositiveButton("Add whisky") { _, _ ->
                                findNavController().navigate(
                                    R.id.action_collection_to_edit,
                                    bundleOf("whiskyId" to -1, "prefillBarcode" to barcode),
                                )
                            }
                            .setNegativeButton("Search instead") { _, _ ->
                                binding.searchView.setQuery(barcode, true)
                            }
                            .show()
                    }
                },
                onFailure = {
                    // Network error — fall back to text search
                    binding.searchView.setQuery(barcode, true)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.load()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
