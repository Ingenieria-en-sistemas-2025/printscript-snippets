package com.printscript.snippets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class SnippetsApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<SnippetsApplication>(*args)
}
