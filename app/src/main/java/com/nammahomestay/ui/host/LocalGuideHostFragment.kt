package com.nammahomestay.ui.host

import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.ui.common.TextCardAdapter
import com.nammahomestay.utils.snack
import com.nammahomestay.viewmodel.HostViewModel
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class LocalGuideHostFragment : BaseShellFragment() {
    private val vm: HostViewModel by viewModels()
    private var mapView: MapView? = null
    private var selectedPoint = GeoPoint(14.8, 74.1)
    private var latestSpots = emptyList<LocalSpot>()
    private lateinit var adapter: TextCardAdapter

    override val screenTitle = "Local Guide"

    override fun build() {
        val map = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(selectedPoint)
        }
        mapView = map
        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectedPoint = p
                    renderMapOverlays()
                    binding.root.snack("Spot location selected")
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }))
        form.addView(map, LinearLayout.LayoutParams(-1, 520))
        text("Tap the map to choose the spot location.")
        button("Use Map Center") {
            selectedPoint = map.mapCenter as GeoPoint
            renderMapOverlays()
        }
        button("+ Add Spot") { openAddSpotDialog() }

        text("Existing Spots")
        adapter = TextCardAdapter { position ->
            latestSpots.getOrNull(position)?.let { openEditSpotDialog(it) }
        }
        val list = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LocalGuideHostFragment.adapter
        }
        form.addView(list)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.spots.collect { spots ->
                latestSpots = spots
                adapter.submit(
                    spots.map {
                        "${it.name} - ${it.distanceKm}km\n${it.description}\nTap to edit or delete"
                    }
                )
                renderMapOverlays()
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

    private fun renderMapOverlays() {
        val map = mapView ?: return
        map.overlays.removeAll { it is Marker }
        latestSpots.forEach { spot ->
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(spot.latitude, spot.longitude)
                title = spot.name
                snippet = spot.description
            })
        }
        map.overlays.add(Marker(map).apply {
            position = selectedPoint
            title = "Selected spot"
            snippet = "New or edited spot will be saved here"
        })
        map.invalidate()
    }

    private fun openAddSpotDialog() {
        val fields = spotFields()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Spot")
            .setView(fields.view)
            .setPositiveButton("Save") { _, _ ->
                val spotName = fields.name.text.toString().trim()
                if (spotName.isBlank()) {
                    binding.root.snack("Enter spot name")
                    return@setPositiveButton
                }
                vm.addSpot(
                    LocalSpot(
                        homestayId = "demo-home",
                        name = spotName,
                        distanceKm = fields.distance.text.toString().toDoubleOrNull() ?: 1.0,
                        description = fields.description.text.toString(),
                        latitude = selectedPoint.latitude,
                        longitude = selectedPoint.longitude
                    )
                ) { ok, error ->
                    binding.root.snack(if (ok) "Spot saved" else error ?: "Could not save spot")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEditSpotDialog(spot: LocalSpot) {
        selectedPoint = GeoPoint(spot.latitude, spot.longitude)
        mapView?.controller?.setCenter(selectedPoint)
        renderMapOverlays()
        val fields = spotFields(spot)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Spot")
            .setView(fields.view)
            .setPositiveButton("Update") { _, _ ->
                val spotName = fields.name.text.toString().trim()
                if (spotName.isBlank()) {
                    binding.root.snack("Enter spot name")
                    return@setPositiveButton
                }
                vm.updateSpot(
                    spot.copy(
                        name = spotName,
                        distanceKm = fields.distance.text.toString().toDoubleOrNull() ?: spot.distanceKm,
                        description = fields.description.text.toString(),
                        latitude = selectedPoint.latitude,
                        longitude = selectedPoint.longitude
                    )
                ) { ok, error ->
                    binding.root.snack(if (ok) "Spot updated" else error ?: "Could not update spot")
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                confirmDeleteSpot(spot)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSpot(spot: LocalSpot) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${spot.name}?")
            .setMessage("This will remove the local guide spot for travellers too.")
            .setPositiveButton("Delete") { _, _ ->
                vm.deleteSpot(spot.id) { ok, error ->
                    binding.root.snack(if (ok) "Spot deleted" else error ?: "Could not delete spot")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun spotFields(spot: LocalSpot? = null): SpotFields {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val name = EditText(requireContext()).apply {
            hint = "Spot name"
            setText(spot?.name.orEmpty())
        }
        val distance = EditText(requireContext()).apply {
            hint = "Distance km"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(spot?.distanceKm?.toString().orEmpty())
        }
        val description = EditText(requireContext()).apply {
            hint = "Description"
            setText(spot?.description.orEmpty())
        }
        box.addView(name)
        box.addView(distance)
        box.addView(description)
        return SpotFields(box, name, distance, description)
    }

    private data class SpotFields(
        val view: LinearLayout,
        val name: EditText,
        val distance: EditText,
        val description: EditText
    )
}
