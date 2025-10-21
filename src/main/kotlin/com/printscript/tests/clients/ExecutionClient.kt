package com.printscript.tests.clients

import org.springframework.stereotype.Component

@Component
class ExecutionClient {
    fun execute(snippetId: Long, inputs: List<String>?): List<String>? {
        // devuelve los mismos inputs como "outputs".
        return inputs
    }


}
