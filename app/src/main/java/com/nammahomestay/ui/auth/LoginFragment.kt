package com.nammahomestay.ui.auth

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.nammahomestay.R
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : BaseShellFragment() {
    private val vm: AuthViewModel by activityViewModels()
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(ApiException::class.java) }
            .onSuccess { vm.signInWithGoogle(it) }
            .onFailure {
                val code = (it as? ApiException)?.statusCode
                binding.root.snack(
                    when (code) {
                        10 -> "Google sign-in config error. Add SHA-1/SHA-256 in Firebase, then re-download google-services.json."
                        12501 -> "Google sign-in cancelled."
                        7 -> "Network error. Check internet and try again."
                        else -> "Google sign-in failed${code?.let { c -> " (code $c)" } ?: ""}."
                    }
                )
            }
    }

    override val screenTitle = "Namma-HomeStay"
    override val screenSubtitle = "Empowering Rural Hospitality"

    override fun build() {
        if (vm.isLoggedIn()) {
            findNavController().navigate(if (vm.role() == "host") R.id.hostDashboardFragment else R.id.browseFragment)
            return
        }
        button("Continue with Google") {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireActivity(), options)
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                state.error?.let { binding.root.snack(it) }
                if (state.done) {
                    if (state.authComplete) {
                        when (state.role) {
                            "host" -> findNavController().navigate(R.id.hostDashboardFragment)
                            "traveller" -> findNavController().navigate(R.id.browseFragment)
                            else -> findNavController().navigate(R.id.roleSelectionFragment)
                        }
                    }
                }
            }
        }
    }
}
