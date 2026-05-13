package com.nammahomestay.data.repository

import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.model.Inquiry
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.model.Menu
import com.nammahomestay.utils.today
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object InMemoryStore {
    val homestays = MutableStateFlow<List<Homestay>>(emptyList())
    val menus = MutableStateFlow<List<Menu>>(emptyList())
    val inquiries = MutableStateFlow(listOf<Inquiry>())
    val spots = MutableStateFlow<List<LocalSpot>>(emptyList())

    private val _newInquiry = MutableSharedFlow<Inquiry>(extraBufferCapacity = 4)
    val newInquiry = _newInquiry.asSharedFlow()

    fun upsertHomestay(home: Homestay) {
        homestays.value = homestays.value.filterNot { it.id == home.id } + home
    }

    fun addMenu(menu: Menu) {
        menus.value = listOf(menu) + menus.value
    }

    fun upsertMenu(menu: Menu) {
        menus.value = listOf(menu) + menus.value.filterNot { it.id == menu.id }
    }

    fun deleteMenu(id: String) {
        menus.value = menus.value.filterNot { it.id == id }
    }

    fun addInquiry(inquiry: Inquiry) {
        inquiries.value = listOf(inquiry) + inquiries.value
        _newInquiry.tryEmit(inquiry)
    }

    fun markRead(id: String) {
        inquiries.value = inquiries.value.map { if (it.id == id) it.copy(read = true) else it }
    }

    fun deleteInquiry(id: String) {
        inquiries.value = inquiries.value.filterNot { it.id == id }
    }

    fun addSpot(spot: LocalSpot) {
        spots.value = listOf(spot) + spots.value
    }

    fun upsertSpot(spot: LocalSpot) {
        spots.value = listOf(spot) + spots.value.filterNot { it.id == spot.id }
    }

    fun deleteSpot(id: String) {
        spots.value = spots.value.filterNot { it.id == id }
    }

    fun observeHomestays() = homestays.asStateFlow()
    fun observeMenus() = menus.asStateFlow()
    fun observeInquiries() = inquiries.asStateFlow()
    fun observeSpots() = spots.asStateFlow()
}
