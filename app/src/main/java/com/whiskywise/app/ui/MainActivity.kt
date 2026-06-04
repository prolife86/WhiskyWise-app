package com.whiskywise.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.whiskywise.app.R
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.databinding.ActivityMainBinding
import com.whiskywise.app.ui.login.LoginActivity
import com.whiskywise.app.util.TokenStore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        // Explicitly suppress the system-provided title/action bar before AppCompat's
        // delegate runs. Without this, AppCompat 1.7+ can install a window decor action
        // bar during feature setup even with a NoActionBar parent theme, causing
        // setSupportActionBar() to throw "This Activity already has an action bar".
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wire up the Toolbar as the support action bar.
        // This is what makes MenuProvider items (Edit / Delete in DetailFragment) visible.
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Top-level destinations: no back arrow on these screens.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.collectionFragment,
                R.id.wishlistFragment,
                R.id.statisticsFragment,
                R.id.settingsFragment,
            )
        )

        // Toolbar title + back arrow automatically follow the nav controller.
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        refreshCurrencySymbol()
    }

    /** Let the nav controller handle the Up (back arrow) button in the toolbar. */
    override fun onSupportNavigateUp(): Boolean =
        findNavController().navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    /** Called from SettingsFragment to log out. */
    fun logout() {
        TokenStore(this).clear()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    /**
     * Silently fetch /api/v1/stats and persist the server's currency symbol.
     * Called on every app open so a server-side currency change is picked up
     * without requiring the user to visit the Statistics tab.
     */
    private fun refreshCurrencySymbol() {
        lifecycleScope.launch {
            WhiskyWiseRepository().getStats().onSuccess { stats ->
                val store = TokenStore(this@MainActivity)
                if (stats.currencySymbol.isNotBlank()) {
                    store.saveCurrencySymbol(stats.currencySymbol)
                }
                if (stats.currencyCode.isNotBlank()) {
                    store.saveCurrencyCode(stats.currencyCode)
                }
            }
        }
    }

    private fun findNavController() =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController
}
