package com.printscript.snippets

import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.enums.Compliance
import com.printscript.snippets.service.SnippetAndSnippetTestsToDto
import io.printscript.contracts.DiagnosticDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class SnippetToDtoTest {

    private fun snippet(): Snippet =
        Snippet(
            id = UUID.randomUUID(),
            ownerId = "auth0|user",
            name = "My Snip",
            description = "Desc",
            language = "printscript",
            languageVersion = "1.1",
            currentVersionId = UUID.randomUUID(),
            lastIsValid = true,
            lastLintCount = 3,
            compliance = Compliance.COMPLIANT,
        )

    private fun version(snippetId: UUID): SnippetVersion =
        SnippetVersion(
            id = UUID.randomUUID(),
            snippetId = snippetId,
            versionNumber = 7,
            contentKey = "k",
            formattedKey = null,
            isFormatted = false,
            isValid = true,
            lintIssues = "[]",
        )

    // ---------- toDetailDto ----------

    @Test
    fun `toDetailDto mapea todos los campos correctamente`() {
        val snip = snippet()
        val ver = version(snip.id!!)
        val dto = SnippetAndSnippetTestsToDto.toDetailDto(snip, ver, "hello world")

        assertEquals(snip.id.toString(), dto.id)
        assertEquals("My Snip", dto.name)
        assertEquals("Desc", dto.description)
        assertEquals("printscript", dto.language)
        assertEquals("1.1", dto.version)
        assertEquals("auth0|user", dto.ownerId)
        assertEquals("hello world", dto.content)
        assertEquals(true, dto.isValid)
        assertEquals(3, dto.lintCount)
    }

    // ---------- toSummaryDto ----------

    @Test
    fun `toSummaryDto mapea todos los campos correctamente`() {
        val snip = snippet()

        val dto = SnippetAndSnippetTestsToDto.toSummaryDto(snip, "owner@example.com")

        assertEquals(snip.id.toString(), dto.id)
        assertEquals("My Snip", dto.name)
        assertEquals("Desc", dto.description)
        assertEquals("printscript", dto.language)
        assertEquals("1.1", dto.version)
        assertEquals("auth0|user", dto.ownerId)
        assertEquals("owner@example.com", dto.ownerEmail)
        assertEquals(true, dto.lastIsValid)
        assertEquals(3, dto.lastLintCount)
        assertEquals("COMPLIANT", dto.compliance)
    }

    // ---------- toApiDiagnostics ----------

    @Test
    fun `toApiDiagnostics convierte correctamente la lista de DiagnosticDto`() {
        val d1 = DiagnosticDto("R1", "msg1", 10, 1)
        val d2 = DiagnosticDto("R2", "msg2", 20, 5)

        val list = SnippetAndSnippetTestsToDto.toApiDiagnostics(listOf(d1, d2))

        assertEquals(2, list.size)

        assertEquals("R1", list[0].ruleId)
        assertEquals("msg1", list[0].message)
        assertEquals(10, list[0].line)
        assertEquals(1, list[0].col)

        assertEquals("R2", list[1].ruleId)
        assertEquals("msg2", list[1].message)
        assertEquals(20, list[1].line)
        assertEquals(5, list[1].col)
    }
}
