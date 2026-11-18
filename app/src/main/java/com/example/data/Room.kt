package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Type-safe Firestore model for a Party Live Room.
 *
 * Every property defines a default value so Firestore's reflection deserializer
 * (`.toObject(Room::class.java)`) has a no-arg constructor.
 */
@Keep
@IgnoreExtraProperties
data class Room(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val activeUsers: Int = 0,
    val speakers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
