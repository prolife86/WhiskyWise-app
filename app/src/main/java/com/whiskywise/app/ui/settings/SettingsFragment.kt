package com.whiskywise.app.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.databinding.FragmentSettingsBinding
import com.whiskywise.app.ui.MainActivity
import com.whiskywise.app.util.TokenStore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: SettingsViewModel by viewModels()

    private lateinit var tokenAdapter: TokenAdapter
    private lateinit var sessionAdapter: SessionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val store = TokenStore(requireContext())
        binding.tvServerUrl.text  = store.getServerUrl() ?: "—"
        binding.tvAppVersion.text = "v${com.whiskywise.app.BuildConfig.VERSION_NAME} (${com.whiskywise.app.BuildConfig.VERSION_CODE})"

        // ── API Tokens ────────────────────────────────────────────────────────
        tokenAdapter = TokenAdapter { token ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Revoke token?")
                .setMessage("Remove \"${token.name}\"? It will no longer have access to the server.")
                .setPositiveButton("Revoke") { _, _ -> vm.revokeToken(token.id) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.rvTokens.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTokens.adapter = tokenAdapter

        // ── Browser Sessions ──────────────────────────────────────────────────
        sessionAdapter = SessionAdapter { session ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Revoke session?")
                .setMessage("This will sign out the browser session from ${session.originIp ?: "unknown IP"}.")
                .setPositiveButton("Revoke") { _, _ -> vm.revokeSession(session.id) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = sessionAdapter

        // ── Observe ───────────────────────────────────────────────────────────
        vm.tokens.observe(viewLifecycleOwner)   { tokens   -> tokenAdapter.submitList(tokens) }
        vm.sessions.observe(viewLifecycleOwner) { sessions -> sessionAdapter.submitList(sessions) }
        vm.error.observe(viewLifecycleOwner)    { err ->
            if (err != null) {
                Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("Your token will be removed from this device.")
                .setPositiveButton("Log out") { _, _ -> (requireActivity() as? MainActivity)?.logout() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        vm.loadAll()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
