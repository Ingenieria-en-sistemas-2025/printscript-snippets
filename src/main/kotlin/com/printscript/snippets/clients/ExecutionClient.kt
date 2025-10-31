package com.printscript.snippets.clients

import org.springframework.stereotype.Component

@Component
class ExecutionClient {
    fun execute(inputs: List<String>?): List<String>? {
        // devuelve los mismos inputs como "outputs".
        return inputs
    }
}
