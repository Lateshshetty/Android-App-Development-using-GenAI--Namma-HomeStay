package com.nammahomestay.data.remote

object SupabaseProvider {
    val rest: SupabaseRestService by lazy { SupabaseRestService.create() }
}
