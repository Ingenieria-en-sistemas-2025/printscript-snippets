package com.printscript.tests.clients

import org.springframework.stereotype.Component

@Component
class SnippetClient {
    fun canWrite(): Boolean = true
    fun canRead(): Boolean = true
}
