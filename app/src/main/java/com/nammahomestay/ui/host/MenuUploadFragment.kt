package com.nammahomestay.ui.host

import android.net.Uri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nammahomestay.data.model.Menu
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.ImagePickerBottomSheet
import com.nammahomestay.ui.common.TextCardAdapter
import com.nammahomestay.ui.common.TextCardRow
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.MenuViewModel
import kotlinx.coroutines.launch

class MenuUploadFragment : BaseShellFragment() {
    private val vm: MenuViewModel by viewModels()
    override val screenTitle = "Today's Menu"
    override val screenSubtitle = "Photograph, type details, publish."

    override fun build() {
        button("Photograph a Dish") {
            ImagePickerBottomSheet { uri -> openDishSheet(uri) }.show(parentFragmentManager, "menu_image_picker")
        }

        val adapter = TextCardAdapter { position ->
            vm.menu.value.getOrNull(position)?.let { openEditDishSheet(it) }
        }
        val list = androidx.recyclerview.widget.RecyclerView(requireContext())
        list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        list.adapter = adapter
        form.addView(list)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.menu.collect { rows ->
                adapter.submitRows(rows.map {
                    TextCardRow(
                        text = "${it.dishName}\n${it.description}\nRs ${it.priceInr}",
                        imageUrl = it.photoUrl
                    )
                })
            }
        }
    }

    private fun openDishSheet(uri: Uri) {
        val view = layoutInflater.inflate(com.nammahomestay.R.layout.fragment_shell, null)
        val box = com.nammahomestay.databinding.FragmentShellBinding.bind(view)
        box.title.text = "Dish Details"
        val name = android.widget.EditText(requireContext()).apply { hint = "Dish name" }
        val desc = android.widget.EditText(requireContext()).apply { hint = "Description" }
        val price = android.widget.EditText(requireContext()).apply {
            hint = "Price"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        box.form.addView(name)
        box.form.addView(desc)
        box.form.addView(price)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Publish to Menu")
            .setView(view)
            .setPositiveButton("Publish") { _, _ ->
                vm.upload(uri) { url, uploadError ->
                    if (url == null) {
                        binding.root.snack(uploadError ?: "Could not upload photo")
                    } else {
                        vm.publish(
                            Menu(
                                homestayId = "demo-home",
                                photoUrl = url,
                                dishName = name.text.toString().ifBlank { "Local Dish" },
                                description = desc.text.toString(),
                                priceInr = price.text.toString().toIntOrNull() ?: 120
                            )
                        ) { ok, error ->
                            binding.root.snack(if (ok) "Dish published!" else error ?: "Could not publish dish")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEditDishSheet(menu: Menu) {
        val view = layoutInflater.inflate(com.nammahomestay.R.layout.fragment_shell, null)
        val box = com.nammahomestay.databinding.FragmentShellBinding.bind(view)
        box.title.text = "Edit Dish"
        val name = android.widget.EditText(requireContext()).apply {
            hint = "Dish name"
            setText(menu.dishName)
        }
        val desc = android.widget.EditText(requireContext()).apply {
            hint = "Description"
            setText(menu.description)
        }
        val price = android.widget.EditText(requireContext()).apply {
            hint = "Price"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(menu.priceInr.toString())
        }
        box.form.addView(name)
        box.form.addView(desc)
        box.form.addView(price)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Dish")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                vm.update(
                    menu.copy(
                        dishName = name.text.toString().ifBlank { "Local Dish" },
                        description = desc.text.toString(),
                        priceInr = price.text.toString().toIntOrNull() ?: menu.priceInr
                    )
                ) { ok, error ->
                    binding.root.snack(if (ok) "Dish updated!" else error ?: "Could not update dish")
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                vm.delete(menu.id) { ok, error ->
                    binding.root.snack(if (ok) "Dish deleted" else error ?: "Could not delete dish")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
