package com.printscript.snippets.redis

enum class Channel(val streamKeyProp: String) {
    FORMATTING("streams.formatting"),
    LINTING("streams.linting"),
}
