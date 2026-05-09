package com.whiskywise.app.ui.wishlist

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentWishlistDetailBinding
import com.whiskywise.app.util.formatPrice

/**
 * Read-only detail screen for a wishlist item.
 *
 * Shows only the fields that make sense for a bottle not yet purchased / tasted:
 * Name, Distillery, Region, Age, ABV, Price, Store, Barcode, and Wishlist Notes.
 *
 * Tasting notes (Nose, Palate, Finish), Score, Status, Radar and Photos are
 * intentionally absent — they belong to the collection detail screen.
 */
class WishlistDetailFragment : Fragment() {

    private var _binding: FragmentWishlistDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: WishlistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWishlistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getInt("whiskyId", -1) ?: -1

        // Toolbar: edit (pencil) and delete (trash) icons.
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        findNavController().navigate(
                            R.id.action_wishlist_detail_to_edit_wishlist,
                            bundleOf("whiskyId" to id),
                        )
                        true
                    }
                    R.id.action_delete -> { confirmDelete(id); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        vm.editItem.observe(viewLifecycleOwner) { w ->
            if (w == null) return@observe

            binding.tvName.text       = w.name
            binding.tvDistillery.text = w.distillery ?: "—"
            binding.tvRegion.text     = w.region ?: "—"
            binding.tvPrice.text      = w.price.formatPrice()
            binding.tvStore.text      = w.store ?: "—"

            if (!w.barcode.isNullOrBlank()) {
                binding.layoutBarcode.visibility = View.VISIBLE
                binding.tvBarcode.text = w.barcode
            } else {
                binding.layoutBarcode.visibility = View.GONE
            }

            binding.tvWishlistNotes.text = w.wishlistNotes?.ifBlank { "—" } ?: "—"
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

        if (id > 0) vm.loadItem(id)
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
