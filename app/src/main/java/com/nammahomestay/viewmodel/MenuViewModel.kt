package com.nammahomestay.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammahomestay.data.model.Menu
import com.nammahomestay.data.repository.MenuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MenuViewModel : ViewModel() {
    private val repo = MenuRepository()
    val menu = repo.observeTodayMenu().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init {
        viewModelScope.launch { runCatching { repo.refresh() } }
    }
    fun publish(menu: Menu, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { repo.publish(menu) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun update(menu: Menu, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { repo.update(menu) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun delete(id: String, done: (Boolean, String?) -> Unit = { _, _ -> }) = viewModelScope.launch {
        runCatching { repo.delete(id) }
            .onSuccess { done(true, null) }
            .onFailure { done(false, it.message) }
    }
    fun upload(uri: Uri, done: (String?, String?) -> Unit) = viewModelScope.launch {
        runCatching { repo.uploadPhoto(uri) }
            .onSuccess { done(it, null) }
            .onFailure { done(null, it.message) }
    }
}
