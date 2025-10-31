package com.printscript.tests.controller

import com.printscript.tests.dto.PageDto
import com.printscript.tests.dto.SnippetDetailDto
import com.printscript.tests.dto.SnippetSummaryDto
import com.printscript.tests.service.SnippetService
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val service: SnippetService,
) {
    @GetMapping("/{snippetId}")
    fun getSnippet(@PathVariable snippetId: UUID): SnippetDetailDto =
        service.getSnippet(snippetId)

    @GetMapping("/all")
    fun listMySnippets(
        principal: JwtAuthenticationToken,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): PageDto<SnippetSummaryDto> {
        val userId = principal.name
        return service.listMySnippets(userId, page, size)
    }
}
