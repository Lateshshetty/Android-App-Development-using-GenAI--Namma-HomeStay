package com.nammahomestay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.model.Menu
import com.nammahomestay.data.repository.HomestayRepository
import com.nammahomestay.data.repository.LocalSpotRepository
import com.nammahomestay.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TravellerViewModel : ViewModel() {
    private val homes = HomestayRepository()
    private val menus = MenuRepository()
    private val spotsRepo = LocalSpotRepository()
    val query = MutableStateFlow("")
    val filter = MutableStateFlow("All")
    val selectedHomestay = MutableStateFlow<Homestay?>(null)
    val selectedMenu = MutableStateFlow<List<Menu>>(emptyList())
    val selectedSpots = MutableStateFlow<List<LocalSpot>>(emptyList())
    val selectedHostPhone = MutableStateFlow("")
    val homestays = combine(homes.observeHomestays(), query, filter) { list, q, f ->
        list.filter { h ->
            val available = h.roomsAvailable && h.rooms > 0
            val search = q.isBlank() || h.village.contains(q, true) || h.district.contains(q, true)
            val chip = when (f) {
                "Verified" -> h.verified
                "Under 500" -> h.rate < 500
                "Under 1000" -> h.rate < 1000
                else -> true
            }
            available && search && chip
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menu = menus.observeTodayMenu().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val spots = spotsRepo.observeSpots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init {
        viewModelScope.launch {
            runCatching { homes.refresh() }
            runCatching { menus.refresh() }
            runCatching { spotsRepo.refresh() }
        }
    }
    fun incrementViews(home: Homestay) = viewModelScope.launch { homes.incrementViews(home.id) }
    fun selectHomestay(home: Homestay) {
        selectedHomestay.value = home
        selectedHostPhone.value = home.phone
        selectedMenu.value = emptyList()
        selectedSpots.value = emptyList()
        viewModelScope.launch {
            if (home.phone.isBlank()) {
                selectedHostPhone.value = runCatching { homes.hostPhone(home.hostId) }
                    .getOrDefault("")
            }
        }
        viewModelScope.launch {
            val rows = runCatching {
                menus.refresh(home.id).ifEmpty { menus.refreshLatest(home.id) }
            }
                .getOrElse { menu.value.filter { it.homestayId == home.id } }
            selectedMenu.value = rows.filter { it.homestayId == home.id }
        }
        viewModelScope.launch {
            val rows = runCatching { spotsRepo.refresh(home.id) }
                .getOrElse { spots.value.filter { it.homestayId == home.id } }
            selectedSpots.value = rows.filter { it.homestayId == home.id }
        }
    }
}
