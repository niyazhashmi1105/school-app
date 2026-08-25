package com.tenderbuds.schoolapp.data

/**
 * Holds the current signed-in session in memory for the life of the process.
 * Deliberately simple (no DI framework yet) — appropriate while the app is
 * still being built screen by screen. Does not survive process death; a
 * real "remember me" would persist the token in EncryptedSharedPreferences.
 */
object SessionManager {
    var token: String? = null
        private set
    var userName: String? = null
        private set

    fun signIn(user: LoggedInUser) {
        token = user.token
        userName = user.name
    }

    fun signOut() {
        token = null
        userName = null
    }
}
