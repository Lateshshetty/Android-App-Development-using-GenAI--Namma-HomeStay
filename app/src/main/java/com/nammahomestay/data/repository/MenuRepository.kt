package com.nammahomestay.data.repository

import android.net.Uri
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.model.Menu
import com.nammahomestay.data.remote.SupabaseRestService
import com.nammahomestay.utils.Constants
import com.nammahomestay.utils.today
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MenuRepository {
    private val api = SupabaseRestService.create()
    private val storage = StorageRepository(api)

    fun observeTodayMenu(homestayId: String = "demo-home"): Flow<List<Menu>> =
        InMemoryStore.observeMenus().map { list ->
            val activeId = activeHomestayId(homestayId)
            list.filter { it.date == today() && it.homestayId == activeId }
        }

    suspend fun refresh(homestayId: String = "demo-home"): List<Menu> {
        val response = api.menus(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.${resolveActiveHomestayId(homestayId)}", "eq.${today()}")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load menu" }
        val rows = response.body().orEmpty().map { it.toMenu() }
        InMemoryStore.menus.value = rows
        return rows
    }

    suspend fun refreshLatest(homestayId: String): List<Menu> {
        val response = api.menusForHomestay(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.${resolveActiveHomestayId(homestayId)}")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load menu" }
        val rows = response.body().orEmpty().map { it.toMenu() }
        InMemoryStore.menus.value = rows
        return rows
    }

    suspend fun uploadPhoto(uri: Uri): String = storage.upload(Constants.MENU_BUCKET, uri)
    suspend fun publish(menu: Menu) {
        val actual = menu.copy(id = UUID.randomUUID().toString(), homestayId = resolveActiveHomestayId(menu.homestayId), date = today())
        val response = api.insertMenu(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not publish menu" }
        InMemoryStore.addMenu(response.body()?.firstOrNull()?.toMenu() ?: actual)
    }

    suspend fun update(menu: Menu) {
        require(menu.id.isNotBlank()) { "Could not update dish without an id" }
        val actual = menu.copy(homestayId = resolveActiveHomestayId(menu.homestayId), date = today())
        val response = api.patchMenu(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), id = "eq.${menu.id}", body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not update menu item" }
        InMemoryStore.upsertMenu(response.body()?.firstOrNull()?.toMenu() ?: actual)
    }

    suspend fun delete(id: String) {
        val response = api.deleteMenu(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$id")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not delete menu item" }
        InMemoryStore.deleteMenu(id)
    }

    private fun activeHomestayId(requested: String): String =
        if (requested == "demo-home") {
            val hostId = savedUserId()
            InMemoryStore.homestays.value.firstOrNull { it.hostId == hostId }?.id
                ?: InMemoryStore.homestays.value.firstOrNull()?.id
                ?: ""
        } else {
            requested
        }

    private suspend fun resolveActiveHomestayId(requested: String): String {
        if (requested != "demo-home" && requested.isNotBlank()) return requested
        activeHomestayId(requested).takeIf { it.isNotBlank() }?.let { return it }
        val hostId = resolveSavedUserId()
        require(hostId.isNotBlank()) { "User profile not synced. Please logout and login again." }
        val response = api.homestaysByHost(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$hostId")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not find your homestay" }
        val home = response.body().orEmpty().firstOrNull()?.toHomestay()
        require(home != null && home.id.isNotBlank()) { "Please publish your homestay before adding dishes." }
        InMemoryStore.upsertHomestay(home)
        return home.id
    }

    private suspend fun resolveSavedUserId(): String {
        savedUserId().takeIf { it.isNotBlank() }?.let { return it }
        val firebaseUid = savedFirebaseUid()
        if (firebaseUid.isBlank()) return ""
        val response = api.getUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$firebaseUid")
        if (!response.isSuccessful) return ""
        return response.body().orEmpty().firstOrNull()?.str("id").orEmpty()
    }
}
