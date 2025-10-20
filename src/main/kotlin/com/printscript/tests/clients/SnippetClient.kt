package com.printscript.tests.clients

import org.springframework.stereotype.Component

@Component
class SnippetClient {
    @Suppress("FunctionOnlyReturningConstant")
    fun canWrite(): Boolean = true
    @Suppress("FunctionOnlyReturningConstant")
    fun canRead(): Boolean = true
}
