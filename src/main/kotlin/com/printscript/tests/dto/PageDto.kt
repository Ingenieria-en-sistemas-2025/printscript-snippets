package com.printscript.tests.dto

data class PageDto<T>(
    val items: List<T>,
    val count: Long,
    val page: Int,
    val pageSize: Int,
)
