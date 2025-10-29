package com.printscript.snippets.service

import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSource
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes

interface SnippetService {
    fun createSnippet(ownerId: String, req: CreateSnippetReq): SnippetDetailDto
    fun getSnippet(snippetId: Long): SnippetDetailDto
    fun updateSnippet(snippetId: Long, req: UpdateSnippetReq): SnippetDetailDto
    fun deleteSnippet(snippetId: Long)
    fun addVersion(snippetId: Long, req: SnippetSource): SnippetDetailDto
    fun listMySnippets(userId: String, page: Int, size: Int): PageDto<SnippetSummaryDto>
    fun shareSnippet(req: ShareSnippetReq)
    fun createTestCase(req: CreateTestReq): TestCaseDto
    fun listTestCases(snippetId: Long): List<TestCaseDto>
    fun deleteTestCase(testCaseId: Long)
    fun runParse(req: ParseReq): ParseRes
    fun runLint(req: LintReq): LintRes
    fun runFormat(req: FormatReq): FormatRes
}
