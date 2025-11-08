package com.printscript.snippets.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("users")
class UserController
@Autowired
constructor(
    private val userService: UserService,
) {
    @GetMapping
    fun getAllUsers(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<User>> {
        val userId = jwt.subject
        return ResponseEntity.ok(userService.getAllOtherUsers(userId))
    }
}
