package com.nammahomestay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.nammahomestay.data.model.Homestay
import com.nammahomestay.data.model.LocalSpot
import com.nammahomestay.data.repository.HomestayRepository
import com.nammahomestay.data.repository.InquiryRepository
import com.nammahomestay.data.repository.LocalSpotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HostViewModel : ViewModel() {
    private val homes = HomestayRepository()
    private val spotsRepo = LocalSpotRepository()
    private val inquiries = InquiryRepository()

    val homestay: StateFlow<Homestay> = homes.observeHostHomestay().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Homestay())
    val spots = spotsRepo.observeSpots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inquiriesList = inquiries.observeInquiries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching { homes.refreshHost() }
            runCatching { spotsRepo.refresh() }
            runCatching { inquiries.refresh() }
        }
    }

    fun save(home: Homestay, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { homes.saveHomestay(home) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun uploadHomestayPhoto(uri: Uri, done: (String?, String?) -> Unit) = viewModelScope.launch {
        runCatching { homes.uploadPhotos(listOf(uri)).firstOrNull() }
            .onSuccess { done(it, null) }
            .onFailure { done(null, it.message) }
    }
    fun toggle(available: Boolean) = viewModelScope.launch { homes.toggleAvailability(available) }
    fun addSpot(spot: LocalSpot, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { spotsRepo.save(spot) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun updateSpot(spot: LocalSpot, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { spotsRepo.update(spot) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun deleteSpot(id: String, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { spotsRepo.delete(id) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
}
