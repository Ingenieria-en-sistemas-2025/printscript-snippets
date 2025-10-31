package com.printscript.tests.dto

data class UpdateSnippetReq(
    val name: String? = null,
    val description: String? = null,
    val language: String? = null,
    val version: String? = null,
    val content: String? = null,
    val source: SnippetSource? = null, // actualiza origen si el user reemplaza el codigo por un file
    val fileName: String? = null,
)

// si mandan content creo nueva SnippetVersion(last+1), corro parse+lint y actualizo ult estado
// si solo cambian name/description actualizo el snippet sin version nueva.
