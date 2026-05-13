package com.nammahomestay.ui.traveller

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nammahomestay.R
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.model.Menu
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.TravellerViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HomestayDetailFragment : BaseShellFragment() {
    private val vm: TravellerViewModel by activityViewModels()
    private var countedView = false
    private var mapView: MapView? = null
    override val screenTitle = ""

    override fun build() {
        binding.title.visibility = View.GONE
        binding.subtitle.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            combine(vm.homestays, vm.selectedHomestay, vm.selectedMenu, vm.selectedSpots, vm.selectedHostPhone) { homes, selected, menu, spots, hostPhone ->
                DetailState(selected ?: homes.firstOrNull(), menu, spots, hostPhone)
            }.collect { state ->
                val home = state.home
                home ?: return@collect
                if (!countedView) {
                    countedView = true
                    vm.incrementViews(home)
                }
                render(home, state.menu, state.spots, state.hostPhone)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    private fun render(home: Homestay, menu: List<Menu>, spots: List<LocalSpot>, hostPhone: String) {
        form.removeAllViews()
        addHero(home)
        addTitleBlock(home)
        addHostCard(home)
        addAbout(home)
        addFacilities(home)
        addMenu(home, menu)
        addLocalGuide(home, spots)
        addActions(home, hostPhone)
    }

    private fun addHero(home: Homestay) {
        val url = home.photos.firstOrNull().orEmpty()
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(color(R.color.colorCardBorder))
        }
        form.addView(image, LinearLayout.LayoutParams(-1, dp(340)).apply { bottomMargin = dp(12) })
        if (url.isNotBlank()) Glide.with(image).load(url).centerCrop().into(image)
    }

    private fun addTitleBlock(home: Homestay) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        val left = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        left.addView(label(home.name.ifBlank { "Homestay" }, 30f, R.color.colorTextPrimary, true))
        left.addView(label("${home.village}, ${home.district}", 15f, R.color.colorTextSecondary))
        row.addView(left, LinearLayout.LayoutParams(0, -2, 1f))

        val price = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        price.addView(label("Rs ${home.rate}", 22f, R.color.colorPrimary, true))
        price.addView(label("per night", 12f, R.color.colorTextSecondary))
        row.addView(price)
        form.addView(row)
    }

    private fun addHostCard(home: Homestay) {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(color(R.color.colorSurfaceElevated))
            strokeColor = color(R.color.colorCardBorder)
            strokeWidth = dp(1)
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val avatar = TextView(requireContext()).apply {
            text = home.hostName.ifBlank { "H" }.take(1).uppercase()
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(color(R.color.colorOnPrimary))
            setBackgroundColor(color(R.color.colorSecondary))
        }
        row.addView(avatar, LinearLayout.LayoutParams(dp(58), dp(58)).apply { rightMargin = dp(14) })
        val copy = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(label("Hosted by ${home.hostName.ifBlank { "Host" }}", 16f, R.color.colorTextPrimary, true))
        copy.addView(label(if (home.verified) "Verified local host" else "Verification pending", 14f, R.color.colorTextSecondary))
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(row)
        form.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
    }

    private fun addAbout(home: Homestay) {
        sectionTitle("About this space")
        text(home.description.ifBlank { "A peaceful rural homestay with local hospitality and home-style comfort." })
    }

    private fun addFacilities(home: Homestay) {
        sectionTitle("Facilities")
        val chips = ChipGroup(requireContext()).apply { isSingleLine = false }
        home.facilities.ifEmpty { listOf("Home Food", "Parking") }.forEach {
            chips.addView(Chip(requireContext()).apply {
                text = it
                setChipBackgroundColorResource(R.color.colorVerifiedBg)
                setTextColor(color(R.color.colorSecondary))
            })
        }
        form.addView(chips, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
    }

    private fun addMenu(home: Homestay, menu: List<Menu>) {
        sectionTitle("Today's Special Menu")
        val rows = menu.ifEmpty { vm.menu.value.filter { it.homestayId == home.id } }
        if (rows.isEmpty()) {
            card("No dishes added by host yet.")
            return
        }
        val scroll = HorizontalScrollView(requireContext()).apply { isHorizontalScrollBarEnabled = false }
        val strip = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        rows.forEach { strip.addView(menuCard(it)) }
        scroll.addView(strip)
        form.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(22) })
    }

    private fun menuCard(item: Menu): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            strokeColor = color(R.color.colorCardBorder)
            strokeWidth = dp(1)
        }
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(color(R.color.colorCardBorder))
        }
        box.addView(image, LinearLayout.LayoutParams(-1, dp(120)))
        if (item.photoUrl.isNotBlank()) Glide.with(image).load(item.photoUrl).centerCrop().into(image)
        box.addView(label(item.dishName.ifBlank { "Local Dish" }, 15f, R.color.colorTextPrimary, true).apply { setPadding(dp(12), dp(10), dp(12), 0) })
        box.addView(label(item.description, 13f, R.color.colorTextSecondary).apply { setPadding(dp(12), dp(4), dp(12), 0) })
        box.addView(label("Rs ${item.priceInr}", 16f, R.color.colorPrimary, true).apply { setPadding(dp(12), dp(8), dp(12), dp(12)) })
        card.addView(box)
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(dp(210), -2).apply { rightMargin = dp(14) }
        }
    }

    private fun addLocalGuide(home: Homestay, spots: List<LocalSpot>) {
        sectionTitle("Location")
        val guideSpots = spots.ifEmpty { vm.spots.value.filter { it.homestayId == home.id } }
        if (guideSpots.isEmpty()) {
            card("Local guide spots will appear here.")
            return
        }
        addLocalGuideMap(guideSpots)
        guideSpots.take(3).forEach { card("${it.name} - ${it.distanceKm}km\n${it.description}") }
    }

    private fun addActions(home: Homestay, hostPhone: String) {
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val call = MaterialButton(requireContext()).apply {
            text = "Call Host"
            setOnClickListener { callHost(hostPhone.ifBlank { home.phone }) }
        }
        val inquiry = MaterialButton(requireContext()).apply {
            text = "Send Inquiry"
            setOnClickListener { findNavController().navigate(R.id.action_detail_to_inquiry) }
        }
        row.addView(call, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(8) })
        row.addView(inquiry, LinearLayout.LayoutParams(0, dp(54), 1.4f))
        form.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(24) })
    }

    private fun addLocalGuideMap(spots: List<LocalSpot>) {
        val first = spots.first()
        val map = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(first.latitude, first.longitude))
        }
        spots.forEach { spot ->
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(spot.latitude, spot.longitude)
                title = spot.name
                snippet = "${spot.distanceKm}km - ${spot.description}"
            })
        }
        mapView = map
        form.addView(map, LinearLayout.LayoutParams(-1, dp(180)).apply { bottomMargin = dp(12) })
    }

    private fun sectionTitle(value: String) {
        form.addView(label(value.uppercase(), 14f, R.color.colorTextPrimary, true).apply {
            letterSpacing = 0.04f
            setPadding(0, dp(10), 0, dp(8))
        })
    }

    private fun label(value: String, size: Float, colorRes: Int, bold: Boolean = false): TextView =
        TextView(requireContext()).apply {
            text = value
            textSize = size
            setTextColor(color(colorRes))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun callHost(rawPhone: String) {
        val phone = rawPhone.dialablePhone()
        if (phone.isBlank()) {
            binding.root.snack("Host phone number is not saved yet")
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
    }

    private fun String.dialablePhone(): String {
        val trimmed = trim()
        val digits = trimmed.filter { it.isDigit() }
        return when {
            trimmed.startsWith("+") && digits.isNotBlank() -> "+$digits"
            digits.length == 10 -> "+91$digits"
            else -> digits
        }
    }

    private data class DetailState(
        val home: Homestay?,
        val menu: List<Menu>,
        val spots: List<LocalSpot>,
        val hostPhone: String
    )
}
