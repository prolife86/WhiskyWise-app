package com.whiskywise.app.ui.collection

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentCollectionBinding

class CollectionFragment : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val vm: CollectionViewModel by viewModels()
    private lateinit var adapter: WhiskyAdapter

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
        listOf(
            null          to binding.chipAll,
            "open"        to binding.chipOpen,
            "stashed"     to binding.chipStashed,
            "retired"     to binding.chipRetired,
        ).forEach { (status, chip) ->
            chip.setOnClickListener {
                vm.currentStatus = status
                vm.load()
            }
        }

        // Search
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean { vm.currentQuery = q; vm.load(); return true }
            override fun onQueryTextChange(q: String?): Boolean { if (q.isNullOrBlank()) { vm.currentQuery = null; vm.load() }; return true }
        })

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_collection_to_edit, bundleOf("whiskyId" to -1))
        }

        vm.load()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
