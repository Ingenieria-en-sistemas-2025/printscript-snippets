package com.printscript.snippets.service

import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.RelationFilter
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSource
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.execution.dto.DiagnosticDto
import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes
import java.util.UUID

interface SnippetService {
    fun createSnippet(ownerId: String, req: CreateSnippetReq): SnippetDetailDto
    fun getSnippet(snippetId: UUID): SnippetDetailDto
    fun updateSnippet(snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto
    fun deleteSnippet(snippetId: UUID)
    fun addVersion(snippetId: UUID, req: SnippetSource): SnippetDetailDto
    fun listMySnippets(userId: String, page: Int, size: Int): PageDto<SnippetSummaryDto>
    fun shareSnippet(req: ShareSnippetReq)
    fun createTestCase(req: CreateTestReq): TestCaseDto
    fun listTestCases(snippetId: UUID): List<TestCaseDto>
    fun deleteTestCase(testCaseId: UUID)
    fun runParse(req: ParseReq): ParseRes
    fun runLint(req: LintReq): LintRes
    fun runFormat(req: FormatReq): FormatRes
    fun listAccessibleSnippets(userId: String, page: Int, size: Int, name: String?, language: String?, valid: Boolean?, relation: RelationFilter, sort: String): PageDto<SnippetSummaryDto>
    fun createSnippetFromFile(ownerId: String, meta: CreateSnippetReq, bytes: ByteArray): SnippetDetailDto
    fun updateSnippetOwnerAware(userId: String, snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto
    fun deleteSnippetOwnerAware(userId: String, snippetId: UUID)
    fun shareSnippetOwnerAware(ownerId: String, req: ShareSnippetReq)
    fun download(snippetId: UUID, formatted: Boolean): ByteArray
    fun filename(snippetId: UUID, formatted: Boolean): String
    fun runOneTestOwnerAware(userId: String, snippetId: UUID, testCaseId: UUID): SingleTestRunResult
    fun saveFormatted(snippetId: UUID, formatted: String)
    fun saveLint(snippetId: UUID, violations: List<DiagnosticDto>)
    fun formatOne(snippetId: UUID): SnippetDetailDto
    fun lintOne(snippetId: UUID): SnippetDetailDto
}
