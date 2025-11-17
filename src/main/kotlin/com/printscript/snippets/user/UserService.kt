package com.printscript.snippets.user

import com.printscript.snippets.user.auth0.IdentityProviderClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class UserService
@Autowired
constructor(
    private val identityProviderClient: IdentityProviderClient,
) {
    fun getAllOtherUsers(userId: String): List<User> {
        val allUsers = identityProviderClient.getAllUsers()
        return allUsers.filter { it.userId != userId }
    }

    @Cacheable("usernames", key = "#userId")
    fun getUsernameById(userId: String): String {
        val user: User = identityProviderClient.getUserById(userId)
        return user.name
    }

    @Cacheable("userEmails", key = "#userId")
    fun getEmailById(userId: String): String {
        val user: User = identityProviderClient.getUserById(userId)
        return user.email
    }
}
