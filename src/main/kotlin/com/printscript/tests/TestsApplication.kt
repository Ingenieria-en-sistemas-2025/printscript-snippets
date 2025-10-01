package com.printscript.tests

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestsApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<TestsApplication>(*args)
}
