package com.printscript.snippets.controller

import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.FileTypeDto
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.RelationFilter
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.dto.RunSnippetInputsReq
import com.printscript.snippets.dto.SaveRulesReq
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.redis.service.BulkRulesService
import com.printscript.snippets.service.SnippetService
import com.printscript.snippets.service.SnippetServiceImpl
import com.printscript.snippets.service.rules.RulesStateService
import io.printscript.contracts.run.RunRes
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
    private val bulkRulesService: BulkRulesService,
    private val rulesStateService: RulesStateService,
    private val snippetService: SnippetServiceImpl,
) {
    private val logger = LoggerFactory.getLogger(SnippetController::class.java)

    @GetMapping("/{snippetId}")
    fun getSnippet(@PathVariable snippetId: UUID): SnippetDetailDto {
        logger.info("GET /snippets/$snippetId")
        return service.getSnippet(snippetId)
    }

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
    ): PageDto<SnippetSummaryDto> {
        logger.info("========================================")
        logger.info("GET /snippets/all CALLED")
        logger.info("========================================")
        logger.info("User (principal.name): ${principal.name}")
        logger.info("Parameters:")
        logger.info("  - page: $page")
        logger.info("  - size: $size")
        logger.info("  - name: $name")
        logger.info("  - language: $language")
        logger.info("  - valid: $valid")
        logger.info("  - relation: $relation")
        logger.info("  - sort: $sort")
        logger.info("========================================")

        val result = service.listAccessibleSnippets(principal.name, page, size, name, language, valid, relation, sort)

        logger.info("Result from service:")
        logger.info("  - count: ${result.count}")
        logger.info("  - items: ${result.items.size}")
        logger.info("========================================")

        return result
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFromEditor(
        principal: JwtAuthenticationToken,
        @RequestBody @Valid req: CreateSnippetReq,
    ): SnippetDetailDto {
        logger.info("========================================")
        logger.info("POST /snippets CALLED")
        logger.info("User (principal.name): ${principal.name}")
        logger.info("Request: name=${req.name}, language=${req.language}, version=${req.version}")
        logger.info("========================================")

        val result = service.createSnippet(principal.name, req)

        logger.info("Snippet created successfully:")
        logger.info("  - id: ${result.id}")
        logger.info("  - ownerId: ${result.ownerId}")
        logger.info("========================================")

        return result
    }

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
        principal: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
        @RequestParam(defaultValue = "false") formatted: Boolean,
    ): ResponseEntity<ByteArray> {
        service.checkPermissions(principal.name, snippetId)
        val bytes = service.download(snippetId, formatted) // trae los bytes del bucket
        val filename = service.filename(snippetId, formatted) // arma el nombre del archivo
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .contentType(MediaType.TEXT_PLAIN)
            .body(bytes)
    }

    @PostMapping("/{snippetId}/tests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTest(
        @PathVariable snippetId: UUID,
        @RequestBody @Valid req: CreateTestReq,
    ): TestCaseDto =
        service.createTestCase(req.copy(snippetId = snippetId.toString()))

    @GetMapping("/{snippetId}/tests")
    fun listTests(
        @PathVariable snippetId: UUID,
    ): List<TestCaseDto> =
        service.listTestCases(snippetId)

    @DeleteMapping("/tests/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTest(
        @PathVariable testCaseId: UUID,
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

    @PutMapping("/rules/format")
    fun saveAndPublishFormat(auth: JwtAuthenticationToken, @RequestBody body: SaveRulesReq): ResponseEntity<Void> {
        val ownerId = auth.token.subject
        rulesStateService.saveFormatState(ownerId, body.rules, body.configText, body.configFormat)
        bulkRulesService.onFormattingRulesChanged(ownerId)
        return ResponseEntity.accepted().build()
    }

    @PutMapping("/rules/linting")
    fun saveAndPublishLint(auth: JwtAuthenticationToken, @RequestBody body: SaveRulesReq): ResponseEntity<Void> {
        val ownerId = auth.token.subject
        rulesStateService.saveLintState(ownerId, body.rules, body.configText, body.configFormat)
        bulkRulesService.onLintingRulesChanged(ownerId)
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/rules/format")
    fun getFmtRules(auth: JwtAuthenticationToken): ResponseEntity<List<RuleDto>> {
        val ownerId = auth.token.subject
        return ResponseEntity.ok(rulesStateService.getFormatAsRules(ownerId))
    }

    @GetMapping("/rules/linting")
    fun getLintRules(auth: JwtAuthenticationToken): ResponseEntity<List<RuleDto>> {
        val ownerId = auth.token.subject
        return ResponseEntity.ok(rulesStateService.getLintAsRules(ownerId))
    }

    @GetMapping("/config/filetypes")
    fun getFileTypes(): List<FileTypeDto> =
        listOf(
            FileTypeDto("printscript", listOf("1.1", "1.0"), "prs"),
        )

    @PostMapping("/run/{id}/format")
    fun formatOne(
        @PathVariable id: UUID,
        auth: JwtAuthenticationToken,
    ): ResponseEntity<SnippetDetailDto> {
        val userId = auth.token.subject
        val dto = service.formatOneOwnerAware(userId, id)
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/run/{id}/lint")
    fun lintOne(
        @PathVariable id: UUID,
        auth: JwtAuthenticationToken,
    ): ResponseEntity<SnippetDetailDto> {
        val userId = auth.token.subject
        val dto = service.lintOneOwnerAware(userId, id)
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{snippetId}/run")
    fun runSnippet(
        auth: JwtAuthenticationToken,
        @PathVariable snippetId: UUID,
        @RequestBody body: RunSnippetInputsReq = RunSnippetInputsReq(),
    ): ResponseEntity<RunRes> {
        val userId = auth.token.subject
        val res = snippetService.runSnippetOwnerAware(userId, snippetId, body?.inputs)
        return ResponseEntity.ok(res)
    }

    // prueba push
}
