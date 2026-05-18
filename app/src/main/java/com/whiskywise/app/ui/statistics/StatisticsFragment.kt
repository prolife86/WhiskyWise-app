package com.whiskywise.app.ui.statistics

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val vm: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.stats.observe(viewLifecycleOwner) { stats ->
            if (stats == null) return@observe

            val total = stats.total
            binding.tvTotal.text      = total.toString()
            binding.tvOpen.text       = stats.open.toString()
            binding.tvStashed.text    = stats.stashed.toString()
            binding.tvFinished.text   = stats.finished.toString()
            binding.tvWishlisted.text = stats.wishlistCount.toString()

            // Breakdown bars — proportional to total (avoid divide-by-zero)
            binding.tvBarStashedCount.text  = stats.stashed.toString()
            binding.tvBarOpenCount.text     = stats.open.toString()
            binding.tvBarFinishedCount.text = stats.finished.toString()

            if (total > 0) {
                setBarWidth(binding.barStashed, stats.stashed, total)
                setBarWidth(binding.barOpen,    stats.open,    total)
                setBarWidth(binding.barFinished, stats.finished, total)
            }
        }

        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        vm.load()
    }

    /**
     * Set a bar's width as a fraction of its parent by posting a layout pass.
     * The parent FrameLayout must already be laid out for this to measure correctly.
     */
    private fun setBarWidth(bar: View, value: Int, total: Int) {
        bar.post {
            val parentWidth = (bar.parent as? View)?.width ?: return@post
            val targetWidth = (parentWidth * value.toFloat() / total).toInt().coerceAtLeast(0)
            bar.layoutParams = bar.layoutParams.also { it.width = targetWidth }
            bar.requestLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
