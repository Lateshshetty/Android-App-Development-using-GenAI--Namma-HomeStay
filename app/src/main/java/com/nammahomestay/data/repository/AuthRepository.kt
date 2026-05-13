package com.nammahomestay.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.JsonObject
import com.nammahomestay.BuildConfig
import com.nammahomestay.data.remote.SupabaseRestService
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("namma_auth", Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val api = SupabaseRestService.create()

    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser
    fun isLoggedIn(): Boolean = currentUser() != null
    fun userRole(): String? = prefs.getString("role", null)
    fun firebaseUid(): String = currentUser()?.uid ?: prefs.getString("firebase_uid", "").orEmpty()
    fun phone(): String = currentUser()?.phoneNumber ?: prefs.getString("phone", "").orEmpty()

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<String?> = runCatching {
        val token = account.idToken ?: error("Google ID token missing. Add SHA-1/SHA-256 to Firebase and re-download google-services.json.")
        val credential = GoogleAuthProvider.getCredential(token, null)
        val user = firebaseAuth.signInWithCredential(credential).await().user ?: error("Firebase user missing")
        syncUserWithSupabase(user)
        getUserRole(user.uid)
    }

    suspend fun syncUserWithSupabase(user: FirebaseUser) {
        val body = JsonObject().apply {
            addProperty("firebase_uid", user.uid)
            addProperty("email", user.email.orEmpty())
            addProperty("name", user.displayName.orEmpty())
            user.phoneNumber?.takeIf { it.isNotBlank() }?.let { addProperty("phone", it) }
        }
        val response = api.upsertUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = body)
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not sync Firebase user to Supabase" }
        val row = response.body()?.firstOrNull()
        prefs.edit()
            .putString("firebase_uid", user.uid)
            .putString("supabase_user_id", row?.str("id").orEmpty())
            .putString("role", row?.str("role").orEmpty().ifBlank { null })
            .putString("phone", user.phoneNumber.orEmpty())
            .apply()
    }

    suspend fun getUserRole(firebaseUid: String): String? {
        val response = api.getUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), "eq.$firebaseUid")
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not fetch user role" }
        val role = response.body()?.firstOrNull()?.str("role")?.ifBlank { null }
        prefs.edit().putString("role", role).apply()
        return role
    }

    suspend fun saveRole(role: String) {
        val uid = firebaseUid()
        require(uid.isNotBlank()) { "Please login again" }
        val body = JsonObject().apply {
            addProperty("firebase_uid", uid)
            phone().takeIf { it.isNotBlank() }?.let { addProperty("phone", it) }
            addProperty("role", role)
        }
        val response = api.upsertUser(BuildConfig.SUPABASE_ANON_KEY, anonAuth(), body = body)
        require(response.isSuccessful) { response.errorBody()?.string() ?: "Could not save role" }
        prefs.edit()
            .putString("role", role)
            .putString("supabase_user_id", response.body()?.firstOrNull()?.str("id").orEmpty())
            .apply()
    }

    fun logout() {
        firebaseAuth.signOut()
        prefs.edit().clear().apply()
    }
}
