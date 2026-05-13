package com.nammahomestay.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nammahomestay.BuildConfig
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SupabaseRestService {
    @PUT("storage/v1/object/{bucket}/{path}")
    suspend fun uploadObject(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("x-upsert") upsert: String = "true",
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<JsonElement>

    @GET("rest/v1/users")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("firebase_uid") firebaseUid: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>

    @GET("rest/v1/users")
    suspend fun getUserById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>

    @POST("rest/v1/users")
    suspend fun upsertUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Query("on_conflict") onConflict: String = "firebase_uid",
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @GET("rest/v1/homestays")
    suspend fun homestays(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<JsonObject>>

    @GET("rest/v1/homestays")
    suspend fun homestaysByHost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("host_id") hostId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<JsonObject>>

    @POST("rest/v1/homestays")
    suspend fun upsertHomestay(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/homestays")
    suspend fun patchHomestay(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("rest/v1/menus")
    suspend fun menus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("homestay_id") homestayId: String,
        @Query("date") date: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<JsonObject>>

    @GET("rest/v1/menus")
    suspend fun menusForHomestay(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("homestay_id") homestayId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10
    ): Response<List<JsonObject>>

    @POST("rest/v1/menus")
    suspend fun insertMenu(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/menus")
    suspend fun patchMenu(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @DELETE("rest/v1/menus")
    suspend fun deleteMenu(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Response<JsonElement>

    @GET("rest/v1/inquiries")
    suspend fun inquiries(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("homestay_id") homestayId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<JsonObject>>

    @POST("rest/v1/inquiries")
    suspend fun insertInquiry(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/inquiries")
    suspend fun patchInquiry(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @DELETE("rest/v1/inquiries")
    suspend fun deleteInquiry(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Response<JsonElement>

    @GET("rest/v1/local_spots")
    suspend fun localSpots(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("homestay_id") homestayId: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>

    @POST("rest/v1/local_spots")
    suspend fun insertSpot(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/local_spots")
    suspend fun patchSpot(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body body: JsonObject
    ): Response<List<JsonObject>>

    @DELETE("rest/v1/local_spots")
    suspend fun deleteSpot(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Response<JsonElement>

    companion object {
        fun create(): SupabaseRestService = Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL.trimEnd('/') + "/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseRestService::class.java)
    }
}
