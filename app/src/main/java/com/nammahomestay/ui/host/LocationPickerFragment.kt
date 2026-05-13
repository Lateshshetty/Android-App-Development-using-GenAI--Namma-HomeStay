package com.nammahomestay.ui.host

import com.nammahomestay.ui.common.BaseShellFragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class LocationPickerFragment : BaseShellFragment() {
    private var map: MapView? = null
    override val screenTitle = "Pick Location"
    override val screenSubtitle = "Drag the map until the center is on the spot."

    override fun build() {
        map = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(14.8, 74.1))
        }
        form.addView(map, android.widget.LinearLayout.LayoutParams(-1, 620))
        val coords = text("Lat: 14.8000, Lng: 74.1000")
        button("Refresh Center Coordinates") {
            val center = map?.mapCenter as? GeoPoint
            if (center != null) coords.text = "Lat: %.4f, Lng: %.4f".format(center.latitude, center.longitude)
        }
        button("Confirm This Location") {
            parentFragmentManager.setFragmentResult("location_picker", android.os.Bundle().apply {
                val center = map?.mapCenter as? GeoPoint ?: GeoPoint(14.8, 74.1)
                putDouble("latitude", center.latitude)
                putDouble("longitude", center.longitude)
            })
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        map?.onPause()
        super.onPause()
    }
}
