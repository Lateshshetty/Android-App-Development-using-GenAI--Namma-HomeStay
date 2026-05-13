package com.nammahomestay.ui.host

import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nammahomestay.R
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.viewmodel.AuthViewModel
import com.nammahomestay.viewmodel.HostViewModel
import kotlinx.coroutines.launch

class HostDashboardFragment : BaseShellFragment() {
    private val vm: HostViewModel by viewModels()
    private val auth: AuthViewModel by activityViewModels()
    override val screenTitle = "Home"
    override fun build() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.homestay.collect { home ->
                form.removeAllViews()
                if (home.id.isBlank()) {
                    text("No homestay profile found for this host.", 20f)
                    text("Create and publish your homestay first. After that, room details, photos, and dishes will save to Supabase.")
                    button("Create Homestay Profile") {
                        findNavController().navigate(R.id.onboardingFlowFragment)
                    }
                    button("Logout") {
                        auth.logout()
                        findNavController().navigate(R.id.loginFragment)
                    }
                    return@collect
                }
                text("Namaste, ${home.hostName}!", 22f)
                card("${home.name}\n${home.village}, ${home.district}\n₹${home.rate} / night • ${home.rooms} rooms")
                switch("Rooms Available", home.roomsAvailable) { vm.toggle(it) }
                text(if (home.verified) "Verified: clean rooms, hygienic bathroom, clean kitchen, safe drinking water" else "Unverified checklist pending")
                button("Edit Profile") { findNavController().navigate(R.id.editProfileFragment) }
                button("Logout") {
                    auth.logout()
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }
    }
}
