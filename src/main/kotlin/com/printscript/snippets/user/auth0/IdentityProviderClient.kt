package com.printscript.snippets.user.auth0

import com.printscript.snippets.user.User

interface IdentityProviderClient {
    fun getAllUsers(): List<User>
    fun getUserById(userId: String): User
}
