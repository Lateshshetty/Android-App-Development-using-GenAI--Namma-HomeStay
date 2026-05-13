package com.nammahomestay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammahomestay.data.model.Inquiry
import com.nammahomestay.data.repository.InquiryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InquiryViewModel : ViewModel() {
    private val repo = InquiryRepository()
    val inquiries = repo.observeInquiries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val realtime = repo.realtimeInquiries()
    init {
        viewModelScope.launch { runCatching { repo.refresh() } }
    }
    fun send(inquiry: Inquiry, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { repo.send(inquiry) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun markRead(id: String) = viewModelScope.launch { repo.markRead(id) }
    fun delete(id: String, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { repo.delete(id) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
}
