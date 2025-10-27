package com.printscript.tests.bucket

interface SnippetAsset {
    fun upload(container: String, key: String, bytes: ByteArray)
    fun download(container: String, key: String): ByteArray
    fun delete(container: String, key: String)
}
