package com.nammahomestay.ui.host

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nammahomestay.R
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.ImagePickerBottomSheet
import com.nammahomestay.utils.Constants
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.HostViewModel

class OnboardingFlowFragment : BaseShellFragment() {
    private val vm: HostViewModel by viewModels()
    private var step = 1
    private var draft = Homestay()
    override val screenTitle get() = "Host Onboarding"
    override val screenSubtitle get() = "Step $step of 5"

    override fun build() = render()

    private fun render() {
        binding.subtitle.text = "Step $step of 5"
        form.removeAllViews()
        when (step) {
            1 -> {
                val name = input("Homestay name", draft.name)
                val host = input("Your name", draft.hostName)
                val village = input("Village", draft.village)
                val district = input("District", draft.district)
                val state = input("State", draft.state.ifBlank { "Karnataka" })
                val phone = input("Phone", draft.phone)
                button("Next") {
                    draft = draft.copy(name = name.text.toString(), hostName = host.text.toString(), village = village.text.toString(), district = district.text.toString(), state = state.text.toString(), phone = phone.text.toString())
                    step = 2; render()
                }
            }
            2 -> {
                text("Upload homestay photos. Storage bucket: homestay-photos.")
                text("Selected photos: ${draft.photos.size}")
                draft.photos.forEachIndexed { index, url ->
                    imageCard(url, "Photo ${index + 1}\nTap to update or delete") {
                        openDraftPhotoActions(index)
                    }
                }
                button("Choose Photo") {
                    ImagePickerBottomSheet { uri ->
                        binding.root.snack("Uploading photo...")
                        vm.uploadHomestayPhoto(uri) { url, error ->
                            if (url == null) {
                                binding.root.snack(error ?: "Photo upload failed")
                            } else {
                                draft = draft.copy(photos = (draft.photos + url).take(8))
                                binding.root.snack("Photo uploaded")
                                render()
                            }
                        }
                    }.show(parentFragmentManager, "homestay_photo_picker")
                }
                button("Next") {
                    if (draft.photos.isEmpty()) {
                        binding.root.snack("Please select at least one photo")
                    } else {
                        step = 3; render()
                    }
                }
            }
            3 -> {
                val rooms = input("Number of rooms", draft.rooms.toString())
                val rate = input("Per night rate in rupees", draft.rate.toString())
                val checks = Constants.FACILITIES.map { checkbox(it) }
                button("Next") {
                    draft = draft.copy(rooms = rooms.text.toString().toIntOrNull() ?: 1, rate = rate.text.toString().toIntOrNull() ?: 800, facilities = checks.filter { it.isChecked }.map { it.text.toString() })
                    step = 4; render()
                }
            }
            4 -> {
                text("Self-Verification (builds trust with travellers)")
                val checks = listOf("Rooms are clean with fresh bed sheets", "Toilet and bathroom are hygienic", "Food is prepared in a clean kitchen", "Safe drinking water is available").map { checkbox(it) }
                button("Next") {
                    draft = draft.copy(verified = checks.all { it.isChecked })
                    step = 5; render()
                }
            }
            else -> {
                val description = input("Description", draft.description)
                button("Publish My Homestay") {
                    vm.save(draft.copy(description = description.text.toString())) { ok, error ->
                        if (ok) {
                            binding.root.snack("Homestay saved to Supabase")
                            findNavController().navigate(R.id.action_onboarding_to_dashboard)
                        } else {
                            binding.root.snack(error ?: "Could not save homestay")
                        }
                    }
                }
            }
        }
    }

    private fun openDraftPhotoActions(index: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage Photo ${index + 1}")
            .setItems(arrayOf("Replace photo", "Delete photo")) { _, which ->
                when (which) {
                    0 -> replaceDraftPhoto(index)
                    1 -> {
                        draft = draft.copy(photos = draft.photos.filterIndexed { photoIndex, _ -> photoIndex != index })
                        binding.root.snack("Photo removed")
                        render()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun replaceDraftPhoto(index: Int) {
        ImagePickerBottomSheet { uri ->
            binding.root.snack("Uploading replacement...")
            vm.uploadHomestayPhoto(uri) { url, error ->
                if (url == null) {
                    binding.root.snack(error ?: "Photo upload failed")
                } else {
                    val updatedPhotos = draft.photos.toMutableList().also { it[index] = url }
                    draft = draft.copy(photos = updatedPhotos)
                    binding.root.snack("Photo updated")
                    render()
                }
            }
        }.show(parentFragmentManager, "replace_draft_homestay_photo_picker")
    }
}
