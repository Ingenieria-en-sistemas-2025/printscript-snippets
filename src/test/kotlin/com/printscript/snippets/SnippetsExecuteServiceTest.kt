package com.printscript.snippets

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetsExecuteService
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnippetsExecuteServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var versionRepo: SnippetVersionRepo

    @Mock
    lateinit var testCaseRepo: TestCaseRepo

    @Mock
    lateinit var assetClient: SnippetAsset

    @Mock
    lateinit var executionClient: SnippetExecution

    @Mock
    lateinit var permissionClient: SnippetPermission

    @InjectMocks
    lateinit var service: SnippetsExecuteService

    // ===== helpers =====

    private fun snippet(
        id: UUID = UUID.randomUUID(),
        ownerId: String = "auth0|owner",
        language: String = "printscript",
        version: String = "1.1",
    ): Snippet {
        val s = org.mockito.Mockito.mock(Snippet::class.java)
        whenever(s.id).thenReturn(id)
        whenever(s.ownerId).thenReturn(ownerId)
        whenever(s.language).thenReturn(language)
        whenever(s.languageVersion).thenReturn(version)
        return s
    }

    private fun version(
        id: UUID = UUID.randomUUID(),
        snippetId: UUID,
        number: Long,
        contentKey: String,
    ): SnippetVersion {
        val v = org.mockito.Mockito.mock(SnippetVersion::class.java)
        whenever(v.id).thenReturn(id)
        whenever(v.snippetId).thenReturn(snippetId)
        whenever(v.versionNumber).thenReturn(number)
        whenever(v.contentKey).thenReturn(contentKey)
        return v
    }

    private fun testCase(
        id: UUID = UUID.randomUUID(),
        snippetId: UUID,
        name: String = "test-1",
        inputs: List<String> = listOf("1"),
        expectedOutputs: List<String> = listOf("2"),
        targetVersionNumber: Long? = 1L,
    ) = TestCase(
        id = id,
        snippetId = snippetId,
        name = name,
        inputs = inputs,
        expectedOutputs = expectedOutputs,
        targetVersionNumber = targetVersionNumber,
    )

    // =========================================================
    // runSnippetOwnerAware
    // =========================================================

    @Test
    fun `runSnippetOwnerAware feliz ejecuta snippet con inputs`() {
        // user == owner  -> evita UnsupportedOperation (READER)
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        val v = version(
            snippetId = snippetId,
            number = 3L,
            contentKey = "owner/$snippetId/v3.ps",
        )
        val code = "print(1);"
        val inputs = listOf("10", "20")
        val outputs = listOf("30")

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(assetClient.download("snippets", "owner/$snippetId/v3.ps"))
            .thenReturn(code.toByteArray(StandardCharsets.UTF_8))

        val runRes = RunRes(outputs = outputs)
        whenever(executionClient.run(any<RunReq>())).thenReturn(runRes)

        val res = service.runSnippetOwnerAware(userId, snippetId, inputs)

        assertIterableEquals(outputs, res.outputs)

        // chequeamos que se llamó correctamente a execution
        verify(executionClient).run(any<RunReq>())
    }

    @Test
    fun `runSnippetOwnerAware lanza NotFound si snippet no existe`() {
        val userId = "auth0|someone"
        val snippetId = UUID.randomUUID()

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.runSnippetOwnerAware(userId, snippetId, null)
        }
    }

    @Test
    fun `runSnippetOwnerAware lanza NotFound si snippet no tiene versiones`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(null)

        assertThrows<NotFound> {
            service.runSnippetOwnerAware(userId, snippetId, emptyList())
        }
    }

    // =========================================================
    // runOneTestOwnerAware
    // =========================================================

    @Test
    fun `runOneTestOwnerAware feliz ejecuta test y mapea resultado`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        val v = version(
            snippetId = snippetId,
            number = 1L,
            contentKey = "owner/$snippetId/v1.ps",
        )
        val code = "println(1);"

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(assetClient.download("snippets", "owner/$snippetId/v1.ps"))
            .thenReturn(code.toByteArray(StandardCharsets.UTF_8))

        val testCaseId = UUID.randomUUID()
        val tc = testCase(
            id = testCaseId,
            snippetId = snippetId,
            inputs = listOf("1"),
            expectedOutputs = listOf("1"),
            targetVersionNumber = 1L,
        )
        whenever(testCaseRepo.findById(testCaseId)).thenReturn(Optional.of(tc))

        val execRes = org.mockito.Mockito.mock(RunSingleTestRes::class.java)
        whenever(execRes.status).thenReturn("OK")
        whenever(execRes.actual).thenReturn(listOf("1"))
        whenever(execRes.mismatchAt).thenReturn(null)
        whenever(execRes.diagnostic).thenReturn(null)

        whenever(executionClient.runSingleTest(any<RunSingleTestReq>())).thenReturn(execRes)

        val result: SingleTestRunResult =
            service.runOneTestOwnerAware(userId, snippetId, testCaseId)

        assertEquals("OK", result.status)
        assertIterableEquals(listOf("1"), result.actual)
        assertIterableEquals(listOf("1"), result.expected)
        assertEquals(null, result.mismatchAt)
        assertEquals(null, result.diagnostic)

        verify(executionClient).runSingleTest(any<RunSingleTestReq>())
    }

    @Test
    fun `runOneTestOwnerAware lanza NotFound si testCase no existe`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))

        val testCaseId = UUID.randomUUID()
        whenever(testCaseRepo.findById(testCaseId)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.runOneTestOwnerAware(userId, snippetId, testCaseId)
        }
    }

    @Test
    fun `runOneTestOwnerAware lanza InvalidRequest si testCase pertenece a otro snippet`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        // el snippet existe
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))

        val v = version(
            snippetId = snippetId,
            number = 1L,
            contentKey = "owner/$snippetId/v1.ps",
        )
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(assetClient.download("snippets", "owner/$snippetId/v1.ps"))
            .thenReturn("print(1);".toByteArray(StandardCharsets.UTF_8))
        val otherSnippetId = UUID.randomUUID()
        val testCaseId = UUID.randomUUID()
        val tc = testCase(
            id = testCaseId,
            snippetId = otherSnippetId, // NO coincide con snippetId
        )
        whenever(testCaseRepo.findById(testCaseId)).thenReturn(Optional.of(tc))

        assertThrows<InvalidRequest> {
            service.runOneTestOwnerAware(userId, snippetId, testCaseId)
        }
    }
}