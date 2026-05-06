package com.whiskywise.app.ui.detail

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
import com.whiskywise.app.databinding.FragmentDetailBinding
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.formatAbv
import com.whiskywise.app.util.formatPrice
import com.whiskywise.app.util.formatScore
import com.whiskywise.app.util.loadWhiskyPhoto

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: DetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getInt("whiskyId") ?: return

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        findNavController().navigate(
                            R.id.action_detail_to_edit,
                            bundleOf("whiskyId" to id),
                        )
                        true
                    }
                    R.id.action_delete -> { confirmDelete(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        vm.whisky.observe(viewLifecycleOwner) { w ->
            if (w == null) return@observe
            binding.tvName.text       = w.name
            binding.tvDistillery.text = w.distillery ?: "—"
            binding.tvRegion.text     = w.region ?: "—"
            binding.tvAge.text        = w.age ?: "—"
            binding.tvAbv.text        = w.abv.formatAbv()
            binding.tvScore.text      = w.score.formatScore()
            binding.tvStatus.text     = w.status?.replaceFirstChar { it.uppercase() } ?: "—"
            binding.tvRetiredBadge.visibility = if (w.retired) View.VISIBLE else View.GONE
            binding.tvPrice.text      = w.price.formatPrice()
            binding.tvStore.text      = w.store ?: "—"
            binding.tvNose.text       = w.nose?.ifBlank { "—" } ?: "—"
            binding.tvPalate.text     = w.palate?.ifBlank { "—" } ?: "—"
            binding.tvFinish.text     = w.finish?.ifBlank { "—" } ?: "—"
            binding.tvNotes.text      = w.notes?.ifBlank { "—" } ?: "—"
            binding.tvFlavor.text     = w.flavorProfile?.replaceFirstChar { it.uppercase() } ?: "—"

            // Barcode — show row only when a value exists
            if (!w.barcode.isNullOrBlank()) {
                binding.layoutBarcode.visibility = View.VISIBLE
                binding.tvBarcode.text = w.barcode
            } else {
                binding.layoutBarcode.visibility = View.GONE
            }

            val ctx       = requireContext()
            val store     = TokenStore(ctx)
            val serverUrl = store.getServerUrl() ?: ""
            val token     = store.getToken() ?: ""
            binding.ivFront.loadWhiskyPhoto(ctx, w.photoFront, serverUrl, token)

            binding.radarView.setValues(
                woody     = w.radarWoody,
                smoky     = w.radarSmoky,
                cereal    = w.radarCereal,
                floral    = w.radarFloral,
                fruity    = w.radarFruity,
                medicinal = w.radarMedicinal,
                fiery     = w.radarFiery,
            )
        }

        vm.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
        }

        vm.load(id)
    }

    private fun confirmDelete() {
        val id = arguments?.getInt("whiskyId") ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete whisky?")
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
