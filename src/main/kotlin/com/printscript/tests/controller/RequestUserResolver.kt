package com.printscript.tests.controller

import jakarta.servlet.http.HttpServletRequest

object RequestUserResolver {
    fun resolveUserId(req: HttpServletRequest): String =
        req.getHeader("X-User-Id") ?: "dev-user"
}
