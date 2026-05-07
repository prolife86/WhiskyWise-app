package com.whiskywise.app.ui.wishlist

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentEditWishlistBinding
import com.whiskywise.app.model.WhiskyRequest

/**
 * Edit screen for wishlist items.
 * Mirrors the server's wishlist_form.html exactly:
 * Name, Distillery, Region, Price, Store, Barcode, Wishlist Notes.
 * No tasting notes, score, status, radar or photos — those only make
 * sense for bottles you've actually opened.
 */
class EditWishlistFragment : Fragment() {

    private var _binding: FragmentEditWishlistBinding? = null
    private val binding get() = _binding!!
    private val vm: WishlistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getInt("whiskyId", -1) ?: -1

        // Trash icon in the toolbar — only shown when editing an existing item.
        if (id > 0) {
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_edit_wishlist, menu)
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId == R.id.action_delete) {
                        confirmDelete(id); return true
                    }
                    return false
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        if (id > 0) {
            vm.loadItem(id)
            vm.editItem.observe(viewLifecycleOwner) { w ->
                if (w == null) return@observe
                binding.etName.setText(w.name)
                binding.etDistillery.setText(w.distillery)
                binding.etRegion.setText(w.region)
                binding.etPrice.setText(w.price?.toString())
                binding.etStore.setText(w.store)
                binding.etBarcode.setText(w.barcode)
                binding.etWishlistNotes.setText(w.wishlistNotes)
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
                findNavController().popBackStack()
            }
        }

        binding.btnSave.setOnClickListener { save(id) }
    }

    private fun save(id: Int) {
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) { binding.etName.error = "Required"; return }

        val req = WhiskyRequest(
            name          = name,
            distillery    = binding.etDistillery.text.toString().trim().ifBlank { null },
            region        = binding.etRegion.text.toString().trim().ifBlank { null },
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
