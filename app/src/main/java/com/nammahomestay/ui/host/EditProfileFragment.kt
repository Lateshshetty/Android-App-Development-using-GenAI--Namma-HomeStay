package com.nammahomestay.ui.host

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.ImagePickerBottomSheet
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.HostViewModel
import kotlinx.coroutines.launch

class EditProfileFragment : BaseShellFragment() {
    private val vm: HostViewModel by viewModels()
    override val screenTitle = "Edit Profile"
    override fun build() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.homestay.collect { home ->
            if (home.id.isBlank()) return@collect
            form.removeAllViews()
            val name = input("Homestay name", home.name)
            val hostName = input("Host name", home.hostName)
            val village = input("Village", home.village)
            val district = input("District", home.district)
            val state = input("State", home.state)
            val phone = input("Phone", home.phone)
            val rooms = input("Available rooms", home.rooms.toString())
            val rate = input("Rate", home.rate.toString())
            val description = input("Description", home.description)
            text("Photos: ${home.photos.size}")
            home.photos.forEachIndexed { index, url ->
                imageCard(url, "Photo ${index + 1}\nTap to update or delete") {
                    openPhotoActions(home, index)
                }
            }
            button("Add Homestay Photo") {
                ImagePickerBottomSheet { uri ->
                    binding.root.snack("Uploading photo...")
                    vm.uploadHomestayPhoto(uri) { url, error ->
                        if (url == null) {
                            binding.root.snack(error ?: "Photo upload failed")
                        } else {
                            vm.save(home.copy(photos = (home.photos + url).take(8))) { ok, saveError ->
                                binding.root.snack(if (ok) "Photo saved to Supabase" else saveError ?: "Could not save photo")
                            }
                        }
                    }
                }.show(parentFragmentManager, "edit_homestay_photo_picker")
            }
            button("Save") {
                vm.save(
                    home.copy(
                        name = name.text.toString(),
                        hostName = hostName.text.toString(),
                        village = village.text.toString(),
                        district = district.text.toString(),
                        state = state.text.toString(),
                        phone = phone.text.toString(),
                        rooms = rooms.text.toString().toIntOrNull() ?: home.rooms,
                        rate = rate.text.toString().toIntOrNull() ?: home.rate,
                        description = description.text.toString()
                    )
                ) { ok, error ->
                    binding.root.snack(if (ok) "Profile saved to Supabase" else error ?: "Could not save profile")
                    if (ok) findNavController().popBackStack()
                }
            }
            }
        }
    }

    private fun openPhotoActions(home: Homestay, index: Int) {
        val currentUrl = home.photos.getOrNull(index) ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage Photo ${index + 1}")
            .setItems(arrayOf("Replace photo", "Delete photo")) { _, which ->
                when (which) {
                    0 -> replacePhoto(home, index)
                    1 -> deletePhoto(home, currentUrl)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun replacePhoto(home: Homestay, index: Int) {
        ImagePickerBottomSheet { uri ->
            binding.root.snack("Uploading replacement...")
            vm.uploadHomestayPhoto(uri) { url, error ->
                if (url == null) {
                    binding.root.snack(error ?: "Photo upload failed")
                } else {
                    val updatedPhotos = home.photos.toMutableList().also { it[index] = url }
                    vm.save(home.copy(photos = updatedPhotos)) { ok, saveError ->
                        binding.root.snack(if (ok) "Photo updated" else saveError ?: "Could not update photo")
                    }
                }
            }
        }.show(parentFragmentManager, "replace_homestay_photo_picker")
    }

    private fun deletePhoto(home: Homestay, url: String) {
        val updatedPhotos = home.photos.filterNot { it == url }
        if (updatedPhotos.isEmpty()) {
            binding.root.snack("Keep at least one homestay photo")
            return
        }
        vm.save(home.copy(photos = updatedPhotos)) { ok, error ->
            binding.root.snack(if (ok) "Photo deleted" else error ?: "Could not delete photo")
        }
    }
}
