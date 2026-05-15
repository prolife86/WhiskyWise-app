package com.whiskywise.app.ui.wishlist

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
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
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.formatPrice
import com.whiskywise.app.util.loadWhiskyPhoto

/**
 * Read-only detail screen for a wishlist item.
 *
 * Shows only the fields that make sense for a bottle not yet purchased / tasted:
 * Name, Distillery, Region, Age, ABV, Price, Store, Barcode, and Wishlist Notes.
 *
 * Tasting notes (Nose, Palate, Finish), Score, Status, Radar and Photos are
 * intentionally absent — they belong to the collection detail screen.
 *
 * Toolbar actions: Move to Collection 🛒, Edit ✏️, Delete 🗑
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
        val store     = TokenStore(requireContext())
        val serverUrl = store.getServerUrl() ?: ""
        val token     = store.getToken() ?: ""

        // Toolbar: move to collection, edit, delete
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_wishlist_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_move_to_collection -> { showPromoteDialog(id); true }
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

            if (!w.photoFront.isNullOrBlank()) {
                binding.ivPhotoCover.visibility = View.VISIBLE
                binding.ivPhotoCover.loadWhiskyPhoto(requireContext(), w.photoFront, serverUrl, token)
            } else {
                binding.ivPhotoCover.visibility = View.GONE
            }
            binding.tvName.text       = w.name
            binding.tvDistillery.text = w.distillery ?: "—"
            binding.tvRegion.text     = w.region ?: "—"
            binding.tvAge.text        = w.age ?: "—"
            binding.tvAbv.text        = w.abv?.let { String.format("%.1f%%", it).replace('.', ',') } ?: "—"
            binding.tvPrice.text      = w.price.formatPrice()
            binding.tvStore.text      = w.store ?: "—"

            if (!w.barcode.isNullOrBlank()) {
                binding.layoutBarcode.visibility = View.VISIBLE
                binding.tvBarcode.text = w.barcode
            } else {
                binding.layoutBarcode.visibility = View.GONE
            }

            binding.tvWishlistNotes.text = w.wishlistNotes?.ifBlank { "—" } ?: "—"

            if (!w.lastTasted.isNullOrBlank()) {
                binding.layoutLastTasted.visibility = View.VISIBLE
                binding.tvLastTasted.text = w.lastTasted
            } else {
                binding.layoutLastTasted.visibility = View.GONE
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

        if (id > 0) vm.loadItem(id)
    }

    private fun showPromoteDialog(id: Int) {
        val statusLabels = arrayOf(
            "📦 Stashed — bought, not yet opened",
            "🔓 Open — currently being enjoyed",
            "🏁 Finished — bottle is empty",
        )
        val statusKeys = arrayOf("stashed", "open", "finished")
        var selectedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move to Collection")
            .setSingleChoiceItems(statusLabels, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Move") { _, _ ->
                val status = statusKeys[selectedIndex]
                vm.promote(id, status) { ok ->
                    if (ok) {
                        Snackbar.make(binding.root, "Moved to collection", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Snackbar.make(binding.root, "Failed to move to collection", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
