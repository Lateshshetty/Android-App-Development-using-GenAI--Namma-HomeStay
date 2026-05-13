package com.nammahomestay.data.repository

import android.net.Uri
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.remote.SupabaseRestService
import com.nammahomestay.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalSpotRepository {
    private val api = SupabaseRestService.create()
    private val storage = StorageRepository(api)

    fun observeSpots(homestayId: String = "demo-home"): Flow<List<LocalSpot>> =
        InMemoryStore.observeSpots().map {
            val activeId = activeHomestayId(homestayId)
            if (activeId.isBlank()) {
                emptyList()
            } else {
                it.filter { spot -> spot.homestayId == activeId }
            }
        }

    suspend fun refresh(homestayId: String = "demo-home"): List<LocalSpot> {
        val activeId = activeHomestayId(homestayId)
        require(activeId.isNotBlank()) { "Could not find homestay for local spots" }
        val response = api.localSpots(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$activeId")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load local spots" }
        val rows = response.body().orEmpty().map { it.toLocalSpot() }
        InMemoryStore.spots.value = rows
        return rows
    }

    suspend fun uploadPhoto(uri: Uri): String = storage.upload(Constants.SPOT_BUCKET, uri)
    suspend fun save(spot: LocalSpot) {
        val actual = spot.copy(id = UUID.randomUUID().toString(), homestayId = activeHomestayId(spot.homestayId))
        require(actual.homestayId.isNotBlank()) { "Please publish your homestay before adding local spots." }
        val response = api.insertSpot(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not save local spot" }
        InMemoryStore.addSpot(response.body()?.firstOrNull()?.toLocalSpot() ?: actual)
    }

    suspend fun update(spot: LocalSpot) {
        require(spot.id.isNotBlank()) { "Could not update spot without an id" }
        val actual = spot.copy(homestayId = activeHomestayId(spot.homestayId))
        require(actual.homestayId.isNotBlank()) { "Please publish your homestay before editing local spots." }
        val response = api.patchSpot(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), id = "eq.${spot.id}", body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not update local spot" }
        InMemoryStore.upsertSpot(response.body()?.firstOrNull()?.toLocalSpot() ?: actual)
    }

    suspend fun delete(id: String) {
        require(id.isNotBlank()) { "Could not delete spot without an id" }
        val response = api.deleteSpot(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$id")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not delete local spot" }
        InMemoryStore.deleteSpot(id)
    }

    private fun activeHomestayId(requested: String): String =
        if (requested == "demo-home") {
            val hostId = savedUserId()
            InMemoryStore.homestays.value.firstOrNull { it.hostId == hostId }?.id.orEmpty()
        } else {
            requested
        }
}
