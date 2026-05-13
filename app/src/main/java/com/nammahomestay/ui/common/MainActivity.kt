package com.nammahomestay.ui.common

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.nammahomestay.R
import com.nammahomestay.databinding.ActivityMainBinding
import com.nammahomestay.viewmodel.HostViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val hostViewModel: HostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val nav = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
        binding.bottomNav.setOnItemSelectedListener {
            val dest = when (it.itemId) {
                R.id.nav_home -> R.id.hostDashboardFragment
                R.id.nav_menu -> R.id.menuUploadFragment
                R.id.nav_inquiries -> R.id.inquiryBoxFragment
                R.id.nav_guide -> R.id.localGuideHostFragment
                else -> R.id.analyticsFragment
            }
            nav.navigate(dest)
            true
        }
        nav.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                R.id.hostDashboardFragment, R.id.menuUploadFragment, R.id.inquiryBoxFragment, R.id.localGuideHostFragment, R.id.analyticsFragment -> View.VISIBLE
                else -> View.GONE
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                hostViewModel.inquiriesList.collect { inquiries ->
                    val unreadCount = inquiries.count { !it.read }
                    val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_inquiries)
                    badge.isVisible = unreadCount > 0
                    if (unreadCount > 0) {
                        badge.number = unreadCount
                    } else {
                        badge.clearNumber()
                    }
                }
            }
        }
    }
}
