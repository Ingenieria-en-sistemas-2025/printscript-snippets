package com.printscript.snippets.controller

import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.RelationFilter
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.service.SnippetService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val service: SnippetService,
) {
    @GetMapping("/{snippetId}")
    fun getSnippet(@PathVariable snippetId: UUID): SnippetDetailDto =
        service.getSnippet(snippetId)

    // caso 5
    @GetMapping("/all")
    fun listAccessible(
        principal: JwtAuthenticationToken,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) valid: Boolean?,
        @RequestParam(defaultValue = "BOTH") relation: RelationFilter,
        @RequestParam(defaultValue = "updatedAt,DESC") sort: String,
    ): PageDto<SnippetSummaryDto> =
        service.listAccessibleSnippets(principal.name, page, size, name, language, valid, relation, sort)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFromEditor(
        principal: JwtAuthenticationToken,
        @RequestBody @Valid req: CreateSnippetReq,
    ): SnippetDetailDto =
        service.createSnippet(principal.name, req)

    @PostMapping(path = ["/file"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createFromFile(
        principal: JwtAuthenticationToken,
        @RequestPart("meta") @Valid meta: CreateSnippetReq,
        @RequestPart("file") file: MultipartFile,
    ): SnippetDetailDto =
        service.createSnippetFromFile(principal.name, meta, file.bytes)


    @PutMapping("/{snippetId}")
    fun updateSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
        @RequestBody @Valid req: UpdateSnippetReq,
    ): SnippetDetailDto =
        service.updateSnippetOwnerAware(principal.name, snippetId, req)

    @DeleteMapping("/{snippetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
    ) = service.deleteSnippetOwnerAware(principal.name, snippetId)

    @PostMapping("/share")
    fun share(
        principal: JwtAuthenticationToken,
        @RequestBody req: ShareSnippetReq,
    ) = service.shareSnippetOwnerAware(principal.name, req)

    @GetMapping("/{snippetId}/download")
    fun download(
        @PathVariable snippetId: UUID,
        @RequestParam(defaultValue = "false") formatted: Boolean,
    ): ResponseEntity<ByteArray> {
        val bytes = service.download(snippetId, formatted)
        val filename = service.filename(snippetId, formatted)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(bytes)
    }

    @PostMapping("/{snippetId}/tests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTest(
        principal: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
        @RequestBody @Valid req: CreateTestReq
    ): TestCaseDto =
        service.createTestCase(req.copy(snippetId = snippetId.toString()))

    @GetMapping("/{snippetId}/tests")
    fun listTests(
        @PathVariable snippetId: UUID
    ): List<TestCaseDto> =
        service.listTestCases(snippetId)

    @DeleteMapping("/tests/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTest(
        @PathVariable testCaseId: UUID
    ) = service.deleteTestCase(testCaseId)

    @PostMapping("/{snippetId}/tests/{testCaseId}/run")
    fun runSingleTest(
        principal: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
        @PathVariable testCaseId: UUID,
    ): SingleTestRunResult = service.runOneTestOwnerAware(
        principal.name,
        snippetId,
        testCaseId,
    )
}
