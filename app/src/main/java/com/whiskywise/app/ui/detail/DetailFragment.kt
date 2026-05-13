package com.whiskywise.app.ui.detail

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.R
import com.whiskywise.app.databinding.FragmentDetailBinding
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.formatAbv
import com.whiskywise.app.util.formatDate
import com.whiskywise.app.util.formatPrice
import com.whiskywise.app.util.formatScore

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: DetailViewModel by viewModels()
    private lateinit var photoPagerAdapter: PhotoPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getInt("whiskyId") ?: return
        val isWishlist = arguments?.getBoolean("isWishlist", false) ?: false

        val store     = TokenStore(requireContext())
        val serverUrl = store.getServerUrl() ?: ""
        val token     = store.getToken() ?: ""

        photoPagerAdapter = PhotoPagerAdapter(requireContext(), serverUrl, token)
        binding.viewPagerPhotos.adapter = photoPagerAdapter

        // Update dot indicators whenever the page changes.
        binding.viewPagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })

        // Inflate toolbar icons and wire each one directly — no overflow menu needed.
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        if (isWishlist) {
                            findNavController().navigate(
                                R.id.action_detail_to_edit_wishlist,
                                bundleOf("whiskyId" to id),
                            )
                        } else {
                            findNavController().navigate(
                                R.id.action_detail_to_edit,
                                bundleOf("whiskyId" to id),
                            )
                        }
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
            binding.tvCreatedAt.text  = w.createdAt.formatDate()
            binding.tvUpdatedAt.text  = w.updatedAt.formatDate()

            if (!w.barcode.isNullOrBlank()) {
                binding.layoutBarcode.visibility = View.VISIBLE
                binding.tvBarcode.text = w.barcode
            } else {
                binding.layoutBarcode.visibility = View.GONE
            }

            // Load photos into pager; hide the container entirely if none exist.
            photoPagerAdapter.submitPhotos(w.photoFront, w.photoBack, w.photoCask, w.updatedAt)
            val photoCount = photoPagerAdapter.count()
            binding.photoContainer.visibility = if (photoCount > 0) View.VISIBLE else View.GONE
            buildDots(photoCount)
            updateDots(0)

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

    // ── Dot indicators ────────────────────────────────────────────────────────

    private fun buildDots(count: Int) {
        binding.dotsContainer.removeAllViews()
        if (count <= 1) return          // no dots needed for 0 or 1 photo
        repeat(count) {
            val dot = ImageView(requireContext()).apply {
                setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.dot_indicator_inactive)
                )
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(6, 0, 6, 0) }
            binding.dotsContainer.addView(dot, params)
        }
    }

    private fun updateDots(activeIndex: Int) {
        val dots = binding.dotsContainer
        for (i in 0 until dots.childCount) {
            val dot = dots.getChildAt(i) as? ImageView ?: continue
            dot.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (i == activeIndex) R.drawable.dot_indicator_active
                    else R.drawable.dot_indicator_inactive,
                )
            )
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

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
