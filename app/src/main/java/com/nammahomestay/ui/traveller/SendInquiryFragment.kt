package com.nammahomestay.ui.traveller

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nammahomestay.data.model.Inquiry
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.InquiryViewModel
import com.nammahomestay.viewmodel.TravellerViewModel

class SendInquiryFragment : BaseShellFragment() {
    private val vm: InquiryViewModel by viewModels()
    private val travellerVm: TravellerViewModel by activityViewModels()
    override val screenTitle = "Send Inquiry"
    override val screenSubtitle = "The host will receive this instantly in the inquiry box."
    override fun build() {
        val name = input("Traveller name")
        val phone = input("Phone")
        val date = input("Check-in date")
        val guests = input("Number of guests", "2")
        val message = input("Message (max 300 characters)")
        button("Call Host") {
            val hostPhone = travellerVm.selectedHostPhone.value.ifBlank { selectedHome()?.phone.orEmpty() }.dialablePhone()
            if (hostPhone.isBlank()) {
                binding.root.snack("Host phone number is not saved yet")
                return@button
            }
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(hostPhone)}")))
        }
        button("Send Inquiry") {
            val homestayId = selectedHome()?.id
            if (homestayId.isNullOrBlank()) {
                binding.root.snack("Please open a homestay from Browse before sending inquiry")
                return@button
            }
            vm.send(Inquiry(homestayId = homestayId, travellerName = name.text.toString(), phone = phone.text.toString(), checkInDate = date.text.toString(), guests = guests.text.toString().toIntOrNull() ?: 2, message = message.text.toString().take(300))) { ok, error ->
                binding.root.snack(if (ok) "Inquiry sent successfully!" else error ?: "Could not send inquiry")
                if (ok) findNavController().popBackStack()
            }
        }
    }

    private fun selectedHome() =
        travellerVm.selectedHomestay.value ?: travellerVm.homestays.value.firstOrNull()

    private fun String.dialablePhone(): String {
        val trimmed = trim()
        val digits = trimmed.filter { it.isDigit() }
        return when {
            trimmed.startsWith("+") && digits.isNotBlank() -> "+$digits"
            digits.length == 10 -> "+91$digits"
            else -> digits
        }
    }
}
