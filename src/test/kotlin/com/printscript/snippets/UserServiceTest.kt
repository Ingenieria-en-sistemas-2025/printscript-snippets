package com.printscript.snippets

import com.printscript.snippets.user.User
import com.printscript.snippets.user.UserService
import com.printscript.snippets.user.auth0.IdentityProviderClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var identityProviderClient: IdentityProviderClient


    @Test
    fun `getAllOtherUsers filtra al usuario actual`() {
        val currentUser = "auth0|me"
        val users = listOf(
            User("auth0|me", "Me", "me@test.com"),
            User("auth0|u1", "Alice", "alice@test.com"),
            User("auth0|u2", "Bob", "bob@test.com"),
        )

        whenever(identityProviderClient.getAllUsers())
            .thenReturn(users)

        val service = UserService(identityProviderClient)

        val result = service.getAllOtherUsers(currentUser)

        assertEquals(2, result.size)
        assertEquals("auth0|u1", result[0].userId)
        assertEquals("auth0|u2", result[1].userId)

        verify(identityProviderClient).getAllUsers()
    }


    @Test
    fun `getUsernameById devuelve name del usuario`() {
        val userId = "auth0|u3"
        val user = User(userId, "Charlie", "charlie@test.com")

        whenever(identityProviderClient.getUserById(userId))
            .thenReturn(user)

        val service = UserService(identityProviderClient)

        val result = service.getUsernameById(userId)

        assertEquals("Charlie", result)
        verify(identityProviderClient).getUserById(userId)
    }
    

    @Test
    fun `getEmailById devuelve email del usuario`() {
        val userId = "auth0|u4"
        val user = User(userId, "Diana", "diana@test.com")

        whenever(identityProviderClient.getUserById(userId))
            .thenReturn(user)

        val service = UserService(identityProviderClient)

        val result = service.getEmailById(userId)

        assertEquals("diana@test.com", result)
        verify(identityProviderClient).getUserById(userId)
    }
}