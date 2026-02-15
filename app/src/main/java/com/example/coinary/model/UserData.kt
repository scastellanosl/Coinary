package com.example.coinary.model

/**
 * UserData: Data model representing the basic profile of an authenticated user.
 * This class stores core identity information retrieved from Firebase Auth or Firestore.
 */
data class UserData(
    val userId: String,
    val username: String?,
    val email: String?
)