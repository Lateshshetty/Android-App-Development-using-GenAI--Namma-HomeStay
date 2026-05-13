package com.nammahomestay.ui.common

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.nammahomestay.R
import java.io.File

/**
 * A styled bottom-sheet with two options:
 * "Take Photo" (camera) and "Choose from Gallery".
 *
 * Business logic and contract unchanged — only visual presentation improved.
 */
class ImagePickerBottomSheet(private val onImage: (Uri) -> Unit) :
    BottomSheetDialogFragment() {

    private var cameraUri: Uri? = null

    private val gallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(onImage)
            dismissAllowingStateLoss()
        }

    private val camera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok) cameraUri?.let(onImage)
            dismissAllowingStateLoss()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dp = { v: Int -> (v * resources.displayMetrics.density + 0.5f).toInt() }

        val poppinsBold: Typeface? = try {
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        } catch (_: Exception) { Typeface.DEFAULT_BOLD }

        val poppins: Typeface? = try {
            ResourcesCompat.getFont(requireContext(), R.font.poppins)
        } catch (_: Exception) { null }

        // Root container
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
            setBackgroundColor(resources.getColor(R.color.colorSurface, null))
        }

        // Sheet title
        root.addView(
            TextView(requireContext()).apply {
                text = "Add a Photo"
                typeface = poppinsBold
                textSize = 18f
                setTextColor(resources.getColor(R.color.colorTextPrimary, null))
                setPadding(dp(4), 0, 0, dp(4))
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }
        )

        // Subtitle
        root.addView(
            TextView(requireContext()).apply {
                text = "Choose how you'd like to add this photo"
                typeface = poppins
                textSize = 13f
                setTextColor(resources.getColor(R.color.colorTextSecondary, null))
                setPadding(dp(4), 0, 0, 0)
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) }
        )

        // Take Photo button (primary — terracotta)
        root.addView(
            MaterialButton(requireContext()).apply {
                text = "📷  Take Photo"
                typeface = poppinsBold
                textSize = 15f
                isAllCaps = false
                cornerRadius =
                    resources.getDimensionPixelSize(R.dimen.button_radius)
                minHeight = resources.getDimensionPixelSize(R.dimen.button_height)
                backgroundTintList =
                    resources.getColorStateList(R.color.colorPrimary, null)
                setTextColor(resources.getColor(R.color.colorOnPrimary, null))
                elevation = resources.getDimension(R.dimen.card_elevation)
                setOnClickListener {
                    val file = File.createTempFile(
                        "photo_${System.currentTimeMillis()}", ".jpg",
                        requireContext().cacheDir
                    )
                    cameraUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    camera.launch(cameraUri)
                }
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        )

        // Choose from Gallery button (outlined)
        root.addView(
            MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "🖼️  Choose from Gallery"
                typeface = poppinsBold
                textSize = 15f
                isAllCaps = false
                cornerRadius =
                    resources.getDimensionPixelSize(R.dimen.button_radius)
                minHeight = resources.getDimensionPixelSize(R.dimen.button_height)
                strokeColor =
                    resources.getColorStateList(R.color.colorPrimary, null)
                setTextColor(resources.getColor(R.color.colorPrimary, null))
                setOnClickListener { gallery.launch("image/*") }
            },
            LinearLayout.LayoutParams(-1, -2)
        )

        return root
    }
}
