package com.budgetify.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.budgetify.app.core.Badge
import com.budgetify.app.core.GameState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Everything that talks to Firebase:
 *  - Firebase Authentication  -> Google single sign-on identity
 *  - Cloud Storage            -> BLOB storage for receipt photos
 *  - Cloud Firestore          -> NoSQL database for gamification (points, streak, badges)
 *
 * Every call degrades gracefully: if google-services.json is missing (e.g. in CI) or the
 * phone is offline, the functions simply return null / false and the app keeps working.
 */
class CloudRepository(private val context: Context) {

    private val tag = "CloudRepository"

    fun isFirebaseReady(): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    /** Firestore/Storage security rules require a signed-in Firebase user; fall back to anonymous. */
    private suspend fun ensureSignedIn(): FirebaseUser? {
        if (!isFirebaseReady()) return null
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it }
        return try {
            auth.signInAnonymously().await().user
        } catch (e: Exception) {
            Log.w(tag, "Anonymous Firebase sign-in failed (offline or disabled): ${e.message}")
            null
        }
    }

    /** SSO: signs the Google account into Firebase Auth so it appears in the Firebase console. */
    suspend fun signInWithGoogle(idToken: String) {
        if (!isFirebaseReady()) return
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
            Log.i(tag, "Signed in to Firebase Auth with Google credential")
        } catch (e: Exception) {
            Log.w(tag, "Firebase Google sign-in failed: ${e.message}")
        }
    }

    fun signOut() {
        if (isFirebaseReady()) FirebaseAuth.getInstance().signOut()
    }

    /** Uploads a receipt image to Firebase Storage and returns its download URL (or null). */
    suspend fun uploadReceipt(userId: Int, file: File): String? {
        if (!file.exists() || ensureSignedIn() == null) return null
        return try {
            val storage = FirebaseStorage.getInstance()
            storage.maxUploadRetryTimeMillis = 20_000
            val ref = storage.reference.child("receipts/$userId/${file.name}")
            ref.putFile(Uri.fromFile(file)).await()
            val url = ref.downloadUrl.await().toString()
            Log.i(tag, "Receipt uploaded: receipts/$userId/${file.name}")
            url
        } catch (e: Exception) {
            Log.w(tag, "Receipt upload failed, will retry on next sync: ${e.message}")
            null
        }
    }

    /**
     * Writes points/streak to users/{id} and each newly unlocked badge to
     * users/{id}/achievements/{badge}. Writes are queued by Firestore when offline,
     * so we deliberately do not await them. Returns true if the writes were issued.
     */
    suspend fun publishGameState(userId: Int, state: GameState, newBadges: Set<Badge>): Boolean {
        if (ensureSignedIn() == null) return false
        return try {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId.toString())
            userDoc.set(
                mapOf(
                    "points" to state.points,
                    "streakDays" to state.streakDays,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).addOnFailureListener { Log.w(tag, "Firestore user write failed: ${it.message}") }

            newBadges.forEach { badge ->
                userDoc.collection("achievements").document(badge.key)
                    .set(mapOf("badge" to badge.key, "unlockedAt" to FieldValue.serverTimestamp()))
                    .addOnFailureListener { Log.w(tag, "Firestore badge write failed: ${it.message}") }
            }
            Log.i(tag, "Published game state (${newBadges.size} new badges) to Firestore")
            true
        } catch (e: Exception) {
            Log.w(tag, "Firestore publish failed: ${e.message}")
            false
        }
    }

    /** Reads the user's unlocked badges back from Firestore (NoSQL read). */
    suspend fun loadUnlockedBadges(userId: Int): Set<Badge> {
        if (ensureSignedIn() == null) return emptySet()
        return try {
            withTimeoutOrNull(8_000) {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users").document(userId.toString())
                    .collection("achievements").get().await()
                snapshot.documents.mapNotNull { Badge.fromKey(it.id) }.toSet()
            } ?: emptySet()
        } catch (e: Exception) {
            Log.w(tag, "Firestore read failed: ${e.message}")
            emptySet()
        }
    }
}
