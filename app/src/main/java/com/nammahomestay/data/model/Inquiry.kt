package com.nammahomestay.data.model

data class Inquiry(
    val id: String = "",
    val homestayId: String = "",
    val travellerName: String = "",
    val phone: String = "",
    val checkInDate: String = "",
    val guests: Int = 2,
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
