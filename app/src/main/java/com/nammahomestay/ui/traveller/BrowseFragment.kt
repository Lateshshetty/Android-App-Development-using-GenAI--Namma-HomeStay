package com.nammahomestay.ui.traveller

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nammahomestay.R
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.TextCardAdapter
import com.nammahomestay.ui.common.TextCardRow
import com.nammahomestay.viewmodel.AuthViewModel
import com.nammahomestay.viewmodel.TravellerViewModel
import kotlinx.coroutines.launch

class BrowseFragment : BaseShellFragment() {
    private val vm: TravellerViewModel by activityViewModels()
    private val auth: AuthViewModel by activityViewModels()
    override val screenTitle = "Browse Homestays"
    override val screenSubtitle = "Search rural coastal stays by village or district."

    override fun build() {
        val search = input("Search by district or village")
        button("Search") { vm.query.value = search.text.toString() }
        button("Logout") {
            auth.logout()
            findNavController().navigate(
                R.id.loginFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
            )
        }

        val chips = ChipGroup(requireContext())
        listOf("All", "Verified", "Under 500", "Under 1000").forEach { label ->
            chips.addView(Chip(requireContext()).apply {
                text = label
                isCheckable = true
                setOnClickListener { vm.filter.value = label }
            })
        }
        form.addView(chips)

        val adapter = TextCardAdapter { position ->
            vm.homestays.value.getOrNull(position)?.let { vm.selectHomestay(it) }
            findNavController().navigate(R.id.action_browse_to_detail)
        }
        val list = androidx.recyclerview.widget.RecyclerView(requireContext())
        list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        list.adapter = adapter
        form.addView(list)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.homestays.collect { rows ->
                adapter.submitRows(rows.map {
                    TextCardRow(
                        text = "${it.name}\n${it.village}, ${it.district}\n${if (it.verified) "Verified" else "Unverified"} - Rs ${it.rate} per night\n${it.facilities.take(3).joinToString(" - ")}",
                        imageUrl = it.photos.firstOrNull().orEmpty()
                    )
                })
            }
        }
    }
}
