package com.printscript.snippets.clients

import org.springframework.stereotype.Component

@Component
class PermissionsClient {
    @Suppress("FunctionOnlyReturningConstant")
    fun canWrite(): Boolean = true

    @Suppress("FunctionOnlyReturningConstant")
    fun canRead(): Boolean = true
}
