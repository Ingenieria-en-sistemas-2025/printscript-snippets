package com.printscript.tests.service

import com.printscript.tests.dto.CreateSnippetReq
import com.printscript.tests.dto.CreateTestReq
import com.printscript.tests.dto.PageDto
import com.printscript.tests.dto.ShareSnippetReq
import com.printscript.tests.dto.SnippetDetailDto
import com.printscript.tests.dto.SnippetSource
import com.printscript.tests.dto.SnippetSummaryDto
import com.printscript.tests.dto.TestCaseDto
import com.printscript.tests.dto.UpdateSnippetReq
import com.printscript.tests.execution.dto.FormatReq
import com.printscript.tests.execution.dto.FormatRes
import com.printscript.tests.execution.dto.LintReq
import com.printscript.tests.execution.dto.LintRes
import com.printscript.tests.execution.dto.ParseReq
import com.printscript.tests.execution.dto.ParseRes

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