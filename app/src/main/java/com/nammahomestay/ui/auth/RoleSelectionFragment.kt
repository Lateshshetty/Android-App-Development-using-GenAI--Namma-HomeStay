package com.nammahomestay.ui.auth

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nammahomestay.R
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.utils.Constants
import com.nammahomestay.viewmodel.AuthViewModel

class RoleSelectionFragment : BaseShellFragment() {
    private val vm: AuthViewModel by activityViewModels()
    override val screenTitle = "Choose your role"
    override val screenSubtitle = "Simple tools for hosts, easy discovery for travellers."
    override fun build() {
        card("I am a Host\nFarmer / Homemaker") {
            vm.saveRole(Constants.ROLE_HOST)
            findNavController().navigate(R.id.action_role_to_onboarding)
        }
        card("I am a Traveller\nTourist / Explorer") {
            vm.saveRole(Constants.ROLE_TRAVELLER)
            findNavController().navigate(R.id.action_role_to_browse)
        }
    }
}
