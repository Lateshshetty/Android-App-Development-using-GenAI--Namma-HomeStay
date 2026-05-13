package com.nammahomestay.data.model

data class Homestay(
    val id: String = "",
    val hostId: String = "",
    val name: String = "",
    val hostName: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = "",
    val phone: String = "",
    val photos: List<String> = emptyList(),
    val rooms: Int = 1,
    val rate: Int = 800,
    val facilities: List<String> = emptyList(),
    val verified: Boolean = false,
    val description: String = "",
    val roomsAvailable: Boolean = true,
    val viewCount: Int = 0,
    val latitude: Double = 14.8,
    val longitude: Double = 74.1
)
