package com.printscript.snippets.dto

data class FileTypeDto(
    val language: String,
    val versions: List<String>,
    val extension: String,
)
