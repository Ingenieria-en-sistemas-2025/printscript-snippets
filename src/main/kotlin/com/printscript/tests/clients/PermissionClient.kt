package com.printscript.tests.clients

import org.springframework.stereotype.Component

@Component
class PermissionClient {
    fun canWrite(userId: String, snippetId: Long): Boolean = true
    fun canRead(userId: String, snippetId: Long): Boolean = true
}
