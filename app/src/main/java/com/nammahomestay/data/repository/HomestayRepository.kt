package com.nammahomestay.data.repository

import android.net.Uri
import com.google.gson.JsonObject
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.remote.SupabaseRestService
import com.nammahomestay.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HomestayRepository {
    private val api = SupabaseRestService.create()
    private val storage = StorageRepository(api)

    fun observeHomestays(): Flow<List<Homestay>> = InMemoryStore.observeHomestays()
    fun observeHostHomestay(): Flow<Homestay> = InMemoryStore.observeHomestays().map { homes ->
        val hostId = savedUserId()
        homes.firstOrNull { it.hostId == hostId } ?: Homestay()
    }

    suspend fun refresh() {
        val response = api.homestays(BuildConfig.SUPABASE_ANON_KEY, anonAuth())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load homestays" }
        val homes = response.body().orEmpty().map { it.toHomestay() }
        InMemoryStore.homestays.value = homes
    }

    suspend fun refreshHost() {
        val hostId = resolveSavedUserId()
        require(hostId.isNotBlank()) { "User profile not synced. Please logout and login again." }
        val response = api.homestaysByHost(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$hostId")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load host homestay" }
        val phone = hostPhone(hostId)
        val hostHomes = response.body().orEmpty().map { row ->
            row.toHomestay().let { home ->
                if (home.phone.isBlank()) home.copy(phone = phone) else home
            }
        }
        InMemoryStore.homestays.value = InMemoryStore.homestays.value.filterNot { it.hostId == hostId } + hostHomes
    }

    suspend fun saveHomestay(home: Homestay) {
        val hostId = resolveSavedUserId()
        require(hostId.isNotBlank()) { "User profile not synced. Please logout and login again." }
        val actual = home.copy(
            id = home.id.ifBlank { UUID.randomUUID().toString() },
            hostId = hostId
        )
        val response = api.upsertHomestay(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not save homestay" }
        syncHostPhone(actual.phone)
        val saved = (response.body()?.firstOrNull()?.toHomestay() ?: actual).let { savedHome ->
            if (savedHome.phone.isBlank()) savedHome.copy(phone = actual.phone) else savedHome
        }
        InMemoryStore.upsertHomestay(saved)
    }

    suspend fun hostPhone(hostId: String): String {
        if (hostId.isBlank()) return ""
        val response = api.getUserById(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$hostId", "phone")
        if (!response.isSuccessful) return ""
        return response.body().orEmpty().firstOrNull()?.str("phone").orEmpty()
    }

    private suspend fun resolveSavedUserId(): String {
        savedUserId().takeIf { it.isNotBlank() }?.let { return it }
        val firebaseUid = savedFirebaseUid()
        if (firebaseUid.isBlank()) return ""
        val response = api.getUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$firebaseUid")
        if (!response.isSuccessful) return ""
        return response.body().orEmpty().firstOrNull()?.str("id").orEmpty()
    }

    private suspend fun syncHostPhone(phone: String) {
        val firebaseUid = savedFirebaseUid()
        if (firebaseUid.isBlank() || phone.isBlank()) return
        val body = JsonObject().apply {
            addProperty("firebase_uid", firebaseUid)
            addProperty("phone", phone)
        }
        val response = api.upsertUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = body)
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not save host phone" }
    }

    suspend fun uploadPhotos(uris: List<Uri>): List<String> = uris.map { storage.upload(Constants.HOMESTAY_BUCKET, it) }
    suspend fun toggleAvailability(available: Boolean) {
        val hostId = resolveSavedUserId()
        val home = InMemoryStore.homestays.value.firstOrNull { it.hostId == hostId } ?: return
        val body = JsonObject().apply { addProperty("available_rooms", if (available) home.rooms.coerceAtLeast(1) else 0) }
        val response = api.patchHomestay(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.${home.id}", body)
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not update availability" }
        InMemoryStore.upsertHomestay(home.copy(roomsAvailable = available))
    }
    suspend fun incrementViews(id: String) {
        InMemoryStore.homestays.value.firstOrNull { it.id == id }?.let {
            val next = it.viewCount + 1
            val body = JsonObject().apply { addProperty("view_count", next) }
            api.patchHomestay(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$id", body)
            InMemoryStore.upsertHomestay(it.copy(viewCount = next))
        }
    }
}
