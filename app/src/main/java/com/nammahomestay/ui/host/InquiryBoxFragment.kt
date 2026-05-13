package com.nammahomestay.ui.host

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.TextCardAdapter
import com.nammahomestay.utils.shortTime
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.InquiryViewModel
import kotlinx.coroutines.launch

class InquiryBoxFragment : BaseShellFragment() {
    private val vm: InquiryViewModel by viewModels()
    override val screenTitle = "Inquiry Box"
    override val screenSubtitle = "Newest first, with live updates."

    override fun build() {
        val adapter = TextCardAdapter { pos ->
            val inquiry = vm.inquiries.value.getOrNull(pos) ?: return@TextCardAdapter
            vm.markRead(inquiry.id)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(inquiry.travellerName.ifBlank { "Traveller Inquiry" })
                .setMessage("${inquiry.message}\n\nPhone: ${inquiry.phone}")
                .setPositiveButton("Call Traveller") { _, _ ->
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${inquiry.phone}")))
                }
                .setNeutralButton("Delete") { _, _ ->
                    confirmDelete(inquiry.id)
                }
                .setNegativeButton("Close", null)
                .show()
        }
        val list = androidx.recyclerview.widget.RecyclerView(requireContext())
        list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        list.adapter = adapter
        form.addView(list)
        viewLifecycleOwner.lifecycleScope.launch {
            vm.inquiries.collect { rows ->
                adapter.submit(
                    rows.map {
                        "${if (it.read) "" else "* "}${it.travellerName}: ${it.message.take(60)}\n${it.createdAt.shortTime()}"
                    }
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.realtime.collect { binding.root.snack("New Inquiry!") }
        }
    }

    private fun confirmDelete(id: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete inquiry?")
            .setMessage("This will remove the inquiry from Supabase.")
            .setPositiveButton("Delete") { _, _ ->
                vm.delete(id) { ok, error ->
                    binding.root.snack(if (ok) "Inquiry deleted" else error ?: "Could not delete inquiry")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
