package com.whiskywise.app.ui.wishlist

import android.os.Bundle
import android.view.*
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
import com.whiskywise.app.util.TokenStore

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val vm: WishlistViewModel by viewModels()
    private lateinit var adapter: WhiskyAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Tap a wishlist item to open the detail view (same as the collection tab)
        adapter = WhiskyAdapter { whisky ->
            findNavController().navigate(
                R.id.action_wishlist_to_detail,
                bundleOf("whiskyId" to whisky.id, "isWishlist" to true),
            )
        }

        // Supply credentials once so the adapter never touches TokenStore per bind.
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

        binding.fab.setOnClickListener { showAddDialog() }

        vm.load()
    }

    /**
     * Reload on resume so that changes made in the detail/edit screen
     * (e.g. moving a wishlist item to the collection) are reflected immediately.
     */
    override fun onResume() {
        super.onResume()
        vm.load()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wishlist, null)
        val etName       = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etDistillery = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDistillery)
        val etRegion     = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRegion)
        val etPrice      = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPrice)
        val etStore      = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etStore)
        val etBarcode    = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBarcode)
        val etNotes      = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNotes)

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
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
