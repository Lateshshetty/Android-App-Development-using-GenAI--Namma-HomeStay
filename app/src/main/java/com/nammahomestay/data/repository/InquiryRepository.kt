package com.nammahomestay.data.repository

import com.google.gson.JsonObject
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.model.Inquiry
import com.nammahomestay.data.remote.SupabaseRestService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

class InquiryRepository {
    private val api = SupabaseRestService.create()

    fun observeInquiries(homestayId: String = "demo-home"): Flow<List<Inquiry>> =
        InMemoryStore.observeInquiries().map {
            val activeId = activeHomestayId(homestayId)
            if (activeId.isBlank()) {
                emptyList()
            } else {
                it.filter { item -> item.homestayId == activeId }
                    .sortedByDescending { item -> item.createdAt }
            }
        }

    fun realtimeInquiries(homestayId: String = "demo-home"): Flow<Inquiry> =
        InMemoryStore.newInquiry.filter { it.homestayId == activeHomestayId(homestayId) }

    suspend fun refresh(homestayId: String = "demo-home") {
        val response = api.inquiries(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.${activeHomestayId(homestayId)}")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not load inquiries" }
        InMemoryStore.inquiries.value = response.body().orEmpty().map { it.toInquiry() }
    }

    suspend fun send(inquiry: Inquiry) {
        val actual = inquiry.copy(id = UUID.randomUUID().toString(), homestayId = activeHomestayId(inquiry.homestayId))
        val response = api.insertInquiry(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = actual.toJson())
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not send inquiry" }
        InMemoryStore.addInquiry(response.body()?.firstOrNull()?.toInquiry() ?: actual)
    }

    suspend fun markRead(id: String) {
        val body = JsonObject().apply { addProperty("is_read", true) }
        val response = api.patchInquiry(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$id", body)
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not mark inquiry as read" }
        InMemoryStore.markRead(id)
    }

    suspend fun delete(id: String) {
        val response = api.deleteInquiry(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$id")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not delete inquiry" }
        InMemoryStore.deleteInquiry(id)
    }

    private fun activeHomestayId(requested: String): String =
        if (requested == "demo-home") {
            val hostId = savedUserId()
            InMemoryStore.homestays.value.firstOrNull { it.hostId == hostId }?.id.orEmpty()
        } else {
            requested
        }
}
