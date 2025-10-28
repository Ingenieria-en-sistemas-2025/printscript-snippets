package com.printscript.tests.dto

data class CreateSnippetReq(
    val name: String,
    val description: String? = null,
    val language: String,
    val version: String,
    val content: String? = null,        //codigo en si
    val source: SnippetSource = SnippetSource.INLINE, //origen del codigo
    val fileName: String? = null,       //si vino por archivo
    val fileSize: Long? = null,         //tamaño en bytes, si vino por archivo
    val mediaType: String? = null
)