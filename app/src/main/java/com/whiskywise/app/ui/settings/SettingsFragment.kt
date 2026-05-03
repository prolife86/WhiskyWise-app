package com.whiskywise.app.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.databinding.FragmentSettingsBinding
import com.whiskywise.app.ui.MainActivity
import com.whiskywise.app.util.TokenStore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val store = TokenStore(requireContext())
        binding.tvServerUrl.text = store.getServerUrl() ?: "—"

        vm.tokens.observe(viewLifecycleOwner) { tokens ->
            binding.tvTokenCount.text = "${tokens.size} active token(s)"
        }
        vm.error.observe(viewLifecycleOwner) { err ->
            if (err != null) Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("Your token will be removed from this device. You can also revoke it from the server.")
                .setPositiveButton("Log out") { _, _ ->
                    (requireActivity() as? MainActivity)?.logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        vm.loadTokens()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
