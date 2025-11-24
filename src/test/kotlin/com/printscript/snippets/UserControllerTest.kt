package com.printscript.snippets

import com.printscript.snippets.user.User
import com.printscript.snippets.user.UserController
import com.printscript.snippets.user.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt

@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock
    lateinit var userService: UserService

    @Test
    fun `getAllUsers devuelve OK y llama al service con el subject del jwt`() {
        // given
        val currentUserId = "auth0|me"

        val otherUsers = listOf(
            User("auth0|u2", "Alice", "alice@test.com"),
            User("auth0|u3", "Bob", "bob@test.com"),
        )

        whenever(userService.getAllOtherUsers(currentUserId))
            .thenReturn(otherUsers)

        val controller = UserController(userService)

        // armamos un Jwt real con sub = currentUserId
        val jwt: Jwt =
            Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", currentUserId)
                .build()

        // when
        val response = controller.getAllUsers(jwt)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(otherUsers, response.body)
        verify(userService).getAllOtherUsers(currentUserId)
    }
}
