package com.nammahomestay.ui.common

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.nammahomestay.R
import com.nammahomestay.databinding.FragmentShellBinding

/**
 * BaseShellFragment — inflates fragment_shell.xml and provides
 * styled factory helpers (text, input, button, card, imageCard,
 * checkbox, switch) for building screens programmatically.
 *
 * IMPORTANT: Do NOT rename binding IDs or remove helpers —
 * all subclass screens depend on this contract.
 */
abstract class BaseShellFragment : Fragment(R.layout.fragment_shell) {

    private var _binding: FragmentShellBinding? = null
    protected val binding get() = _binding!!
    protected val form get() = binding.form

    abstract val screenTitle: String
    open val screenSubtitle: String = ""

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentShellBinding.bind(view)
        binding.title.text = screenTitle
        if (screenSubtitle.isBlank()) {
            binding.subtitle.visibility = View.GONE
        } else {
            binding.subtitle.text = screenSubtitle
            binding.subtitle.visibility = View.VISIBLE
        }
        build()
    }

    abstract fun build()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ─── Style helpers ───────────────────────────────────────────────────────

    /** Lazily load Poppins Regular, fall back to system default. */
    private fun poppins(): Typeface? = try {
        ResourcesCompat.getFont(requireContext(), R.font.poppins)
    } catch (_: Exception) { null }

    /** Lazily load Poppins SemiBold, fall back to BOLD. */
    private fun poppinsBold(): Typeface? = try {
        ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
    } catch (_: Exception) { Typeface.DEFAULT_BOLD }

    /** Convert dp → px using the current display density. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    // ─── Factory methods ─────────────────────────────────────────────────────

    /**
     * Adds a styled [TextView] to [form].
     * @param size  sp size; ≥20 uses SemiBold + primary colour.
     */
    protected fun text(value: String, size: Float = 15f): TextView =
        TextView(requireContext()).apply {
            text = value
            textSize = size
            typeface = if (size >= 20f) poppinsBold() else poppins()
            setTextColor(
                resources.getColor(
                    if (size >= 20f) R.color.colorTextPrimary else R.color.colorTextSecondary,
                    null
                )
            )
            setPadding(0, dp(6), 0, dp(6))
            form.addView(this)
        }

    /**
     * Adds a styled [EditText] to [form].
     */
    protected fun input(hint: String, value: String = ""): EditText =
        EditText(requireContext()).apply {
            this.hint = hint
            setText(value)
            typeface = poppins()
            textSize = 15f
            minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = resources.getDrawable(R.drawable.bg_input_field, null)
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            setHintTextColor(resources.getColor(R.color.colorTextHint, null))
            form.addView(
                this,
                LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(4)
                    bottomMargin = dp(12)
                }
            )
        }

    /**
     * Adds a full-width pill [MaterialButton] to [form].
     */
    protected fun button(label: String, click: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = label
            typeface = poppinsBold()
            textSize = 15f
            isAllCaps = false
            cornerRadius = resources.getDimensionPixelSize(R.dimen.button_radius)
            minHeight = resources.getDimensionPixelSize(R.dimen.button_height)
            setPadding(dp(24), dp(0), dp(24), dp(0))
            backgroundTintList =
                resources.getColorStateList(R.color.colorPrimary, null)
            setTextColor(resources.getColor(R.color.colorOnPrimary, null))
            elevation = dp(2).toFloat()
            setOnClickListener { click() }
            form.addView(
                this,
                LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(4)
                    bottomMargin = dp(12)
                }
            )
        }

    /**
     * Adds a rounded [MaterialCardView] with a text label to [form].
     * Optionally clickable.
     */
    protected fun card(label: String, click: (() -> Unit)? = null): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            radius = resources.getDimension(R.dimen.card_radius)
            cardElevation = resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(resources.getColor(R.color.colorSurface, null))
            strokeColor = resources.getColor(R.color.colorCardBorder, null)
            strokeWidth = dp(1)
        }
        val tv = TextView(requireContext()).apply {
            text = label
            typeface = poppins()
            textSize = 15f
            setLineSpacing(dp(3).toFloat(), 1.0f)
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        card.addView(tv)
        click?.let { card.setOnClickListener { it() } }
        form.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        )
        return card
    }

    /**
     * Adds a [MaterialCardView] with a photo on top and a label below.
     * Loads [imageUrl] via Glide. Optionally clickable.
     */
    protected fun imageCard(
        imageUrl: String,
        label: String,
        click: (() -> Unit)? = null
    ): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            radius = resources.getDimension(R.dimen.card_radius)
            cardElevation = resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(resources.getColor(R.color.colorSurface, null))
            strokeColor = resources.getColor(R.color.colorCardBorder, null)
            strokeWidth = dp(1)
        }
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(resources.getColor(R.color.shimmer_base, null))
        }
        box.addView(image, LinearLayout.LayoutParams(-1, dp(200)))
        // Subtle warm divider line between image and text
        val divider = View(requireContext()).apply {
            setBackgroundColor(resources.getColor(R.color.colorDivider, null))
        }
        box.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        box.addView(
            TextView(requireContext()).apply {
                text = label
                typeface = poppins()
                textSize = 14f
                setLineSpacing(dp(2).toFloat(), 1.0f)
                setTextColor(resources.getColor(R.color.colorTextPrimary, null))
                minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
                setPadding(dp(16), dp(12), dp(16), dp(14))
            }
        )
        card.addView(box)
        click?.let { card.setOnClickListener { it() } }
        form.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        )
        Glide.with(image).load(imageUrl).centerCrop().into(image)
        return card
    }

    /**
     * Adds a [MaterialCheckBox] to [form].
     */
    protected fun checkbox(label: String): MaterialCheckBox =
        MaterialCheckBox(requireContext()).apply {
            text = label
            typeface = poppins()
            textSize = 15f
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
            form.addView(
                this,
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }
            )
        }

    /**
     * Adds a [MaterialSwitch] to [form].
     */
    protected fun switch(
        label: String,
        checked: Boolean,
        change: (Boolean) -> Unit
    ): MaterialSwitch =
        MaterialSwitch(requireContext()).apply {
            text = label
            typeface = poppins()
            textSize = 15f
            isChecked = checked
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
            setOnCheckedChangeListener { _, value -> change(value) }
            form.addView(this)
        }
}
