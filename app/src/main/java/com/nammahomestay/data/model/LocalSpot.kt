package com.nammahomestay.data.model

data class LocalSpot(
    val id: String = "",
    val homestayId: String = "",
    val name: String = "",
    val distanceKm: Double = 1.0,
    val photoUrl: String = "",
    val description: String = "",
    val latitude: Double = 14.8,
    val longitude: Double = 74.1
)
