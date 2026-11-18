package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore repository handling CRUD operations for [Room] documents
 * in the `party_rooms` collection using the provisioned enterprise database ID.
 */
class RoomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeSpeakers(speakers: List<String>): List<String> =
        speakers
            .map { it.trim().take(MAX_SPEAKER_NAME_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_SPEAKERS)

    // --- CREATE ---

    suspend fun createRoom(
        name: String,
        activeUsers: Int = 0,
        speakers: List<String> = emptyList()
    ): Result<Room> {
        var path = COLLECTION_PARTY_ROOMS
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(MAX_ROOM_NAME_LENGTH)
            require(cleanName.length >= MIN_ROOM_NAME_LENGTH) {
                "Room name must be at least $MIN_ROOM_NAME_LENGTH characters."
            }
            val safeActiveUsers = activeUsers.coerceIn(0, MAX_ACTIVE_USERS)
            val safeSpeakers = sanitizeSpeakers(speakers)

            val docRef = db.collection(COLLECTION_PARTY_ROOMS).document()
            path = docRef.path

            val payload = mapOf(
                "ownerId" to uid,
                "name" to cleanName,
                "activeUsers" to safeActiveUsers,
                "speakers" to safeSpeakers,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            Result.success(
                Room(
                    id = docRef.id,
                    ownerId = uid,
                    name = cleanName,
                    activeUsers = safeActiveUsers,
                    speakers = safeSpeakers
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- READ ---

    suspend fun getRoom(roomId: String): Result<Room?> {
        val path = "$COLLECTION_PARTY_ROOMS/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank." }
            val snapshot = db.collection(COLLECTION_PARTY_ROOMS)
                .document(roomId)
                .get()
                .await()
            val room = if (snapshot.exists()) {
                snapshot.toObject(
                    Room::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeOwnedRooms(ownerId: String): Flow<List<Room>> = flow {
        val uid = requireUserId()
        check(uid == ownerId) { "Refusing cross-user room observation." }
        emitAll(
            db.collection(COLLECTION_PARTY_ROOMS)
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Room::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_PARTY_ROOMS)
                    }
                    throw error
                }
        )
    }

    // --- UPDATE ---

    suspend fun updateRoom(
        roomId: String,
        name: String,
        activeUsers: Int,
        speakers: List<String>
    ): Result<Unit> {
        val path = "$COLLECTION_PARTY_ROOMS/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank." }
            val cleanName = name.trim().take(MAX_ROOM_NAME_LENGTH)
            require(cleanName.length >= MIN_ROOM_NAME_LENGTH) {
                "Room name must be at least $MIN_ROOM_NAME_LENGTH characters."
            }
            val safeActiveUsers = activeUsers.coerceIn(0, MAX_ACTIVE_USERS)
            val safeSpeakers = sanitizeSpeakers(speakers)

            db.collection(COLLECTION_PARTY_ROOMS)
                .document(roomId)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "activeUsers" to safeActiveUsers,
                        "speakers" to safeSpeakers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateActiveUsers(roomId: String, activeUsers: Int): Result<Unit> {
        val path = "$COLLECTION_PARTY_ROOMS/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank." }
            val safeActiveUsers = activeUsers.coerceIn(0, MAX_ACTIVE_USERS)
            db.collection(COLLECTION_PARTY_ROOMS)
                .document(roomId)
                .update(
                    mapOf(
                        "activeUsers" to safeActiveUsers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSpeakers(roomId: String, speakers: List<String>): Result<Unit> {
        val path = "$COLLECTION_PARTY_ROOMS/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank." }
            val safeSpeakers = sanitizeSpeakers(speakers)
            db.collection(COLLECTION_PARTY_ROOMS)
                .document(roomId)
                .update(
                    mapOf(
                        "speakers" to safeSpeakers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DELETE ---

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        val path = "$COLLECTION_PARTY_ROOMS/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank." }
            db.collection(COLLECTION_PARTY_ROOMS)
                .document(roomId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_PARTY_ROOMS = "party_rooms"
        const val MIN_ROOM_NAME_LENGTH = 2
        const val MAX_ROOM_NAME_LENGTH = 60
        const val MAX_ACTIVE_USERS = 100_000
        const val MAX_SPEAKERS = 8
        const val MAX_SPEAKER_NAME_LENGTH = 40
    }
}
