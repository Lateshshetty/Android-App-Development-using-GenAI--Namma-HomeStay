package com.nammahomestay.data.repository

import android.net.Uri
import com.nammahomestay.BuildConfig
import com.nammahomestay.NammaApp
import com.nammahomestay.data.remote.SupabaseRestService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class StorageRepository(private val api: SupabaseRestService = SupabaseRestService.create()) {
    suspend fun upload(bucket: String, uri: Uri): String {
        val resolver = NammaApp.appContext.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read selected image")
        val path = "${UUID.randomUUID()}.jpg"
        val response = api.uploadObject(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            authorization = anonAuth(),
            bucket = bucket,
            path = path,
            body = bytes.toRequestBody("image/jpeg".toMediaType())
        )
        require(response.isSuccessful) {
            val body = response.errorBody()?.string().orEmpty()
            if (body.contains("Bucket not found", ignoreCase = true)) {
                "Supabase bucket '$bucket' not found. Create public buckets homestay-photos, menu-photos, and spot-photos."
            } else {
                body.ifBlank { "Could not upload image" }
            }
        }
        return "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/$bucket/$path"
    }
}
