package com.nammahomestay.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nammahomestay.NammaApp
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.model.Inquiry
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.model.Menu
import java.time.Instant

internal fun anonAuth() = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
internal fun tokenAuth(token: String?) = "Bearer ${token?.takeIf { it.isNotBlank() } ?: BuildConfig.SUPABASE_ANON_KEY}"
internal fun savedUserId(): String =
    NammaApp.appContext.getSharedPreferences("namma_auth", android.content.Context.MODE_PRIVATE)
        .getString("supabase_user_id", "")
        .orEmpty()
internal fun savedFirebaseUid(): String =
    NammaApp.appContext.getSharedPreferences("namma_auth", android.content.Context.MODE_PRIVATE)
        .getString("firebase_uid", "")
        .orEmpty()

internal fun JsonObject.str(name: String, fallback: String = "") = get(name)?.takeUnless { it.isJsonNull }?.asString ?: fallback
internal fun JsonObject.int(name: String, fallback: Int = 0) = get(name)?.takeUnless { it.isJsonNull }?.asInt ?: fallback
internal fun JsonObject.double(name: String, fallback: Double = 0.0) = get(name)?.takeUnless { it.isJsonNull }?.asDouble ?: fallback
internal fun JsonObject.bool(name: String, fallback: Boolean = false) = get(name)?.takeUnless { it.isJsonNull }?.asBoolean ?: fallback

internal fun JsonObject.stringList(name: String): List<String> =
    get(name)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { if (it.isJsonNull) null else it.asString } ?: emptyList()

internal fun List<String>.toJsonArray() = JsonArray().also { arr -> forEach { arr.add(it) } }

internal fun Homestay.toJson() = JsonObject().apply {
    if (id.isNotBlank()) addProperty("id", id)
    addProperty("host_id", hostId)
    addProperty("name", name)
    addProperty("village", village)
    addProperty("district", district)
    addProperty("state", state)
    add("photos", photos.toJsonArray())
    addProperty("available_rooms", rooms)
    addProperty("per_night_rate", rate)
    add("facilities", facilities.toJsonArray())
    addProperty("is_verified", verified)
    addProperty("verify_cleanliness", verified)
    addProperty("verify_hygiene", verified)
    addProperty("verify_safety", verified)
    addProperty("verify_water", verified)
    addProperty("description", description)
    addProperty("view_count", viewCount)
    addProperty("latitude", latitude)
    addProperty("longitude", longitude)
}

internal fun JsonObject.toHomestay() = Homestay(
    id = str("id"),
    hostId = str("host_id"),
    name = str("name"),
    hostName = str("host_name", "Host"),
    village = str("village"),
    district = str("district"),
    state = str("state"),
    phone = str("phone"),
    photos = stringList("photos"),
    rooms = int("available_rooms", 1),
    rate = int("per_night_rate", 800),
    facilities = stringList("facilities"),
    verified = bool("is_verified"),
    description = str("description"),
    roomsAvailable = int("available_rooms", 1) > 0,
    viewCount = int("view_count"),
    latitude = double("latitude", 14.8),
    longitude = double("longitude", 74.1)
)

internal fun Menu.toJson() = JsonObject().apply {
    addProperty("homestay_id", homestayId)
    addProperty("photo_url", photoUrl)
    addProperty("item_name", dishName)
    addProperty("description", description)
    addProperty("price", priceInr)
    addProperty("date", date)
    addProperty("is_available", true)
}

internal fun JsonObject.toMenu() = Menu(
    id = str("id"),
    homestayId = str("homestay_id"),
    photoUrl = str("photo_url"),
    dishName = str("item_name"),
    description = str("description"),
    priceInr = int("price", 120),
    date = str("date")
)

internal fun Inquiry.toJson() = JsonObject().apply {
    addProperty("homestay_id", homestayId)
    val travellerId = savedUserId()
    if (travellerId.isNotBlank()) addProperty("traveller_id", travellerId)
    addProperty("traveller_name", travellerName)
    addProperty("traveller_phone", phone)
    addProperty("message", message)
    addProperty("is_read", read)
}

internal fun JsonObject.toInquiry() = Inquiry(
    id = str("id"),
    homestayId = str("homestay_id"),
    travellerName = str("traveller_name"),
    phone = str("traveller_phone"),
    checkInDate = "",
    guests = 2,
    message = str("message"),
    createdAt = runCatching { Instant.parse(str("created_at")).toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
    read = bool("is_read")
)

internal fun LocalSpot.toJson() = JsonObject().apply {
    addProperty("homestay_id", homestayId)
    addProperty("name", name)
    addProperty("distance_km", distanceKm)
    addProperty("photo_url", photoUrl)
    addProperty("description", description)
    addProperty("latitude", latitude)
    addProperty("longitude", longitude)
}

internal fun JsonObject.toLocalSpot() = LocalSpot(
    id = str("id"),
    homestayId = str("homestay_id"),
    name = str("name"),
    distanceKm = double("distance_km", 1.0),
    photoUrl = str("photo_url"),
    description = str("description"),
    latitude = double("latitude", 14.8),
    longitude = double("longitude", 74.1)
)
