package com.printscript.snippets

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetTestService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnippetTestServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var testCaseRepo: TestCaseRepo

    @Mock
    lateinit var permissionClient: SnippetPermission

    @InjectMocks
    lateinit var service: SnippetTestService

    private fun snippet(ownerId: String): Snippet {
        val sn = mock<Snippet>()
        whenever(sn.ownerId).thenReturn(ownerId)
        return sn
    }

    @Test
    fun `createTestCase guarda entidad y devuelve DTO`() {
        val ownerId = "auth0|owner-tests"
        val snippetId = UUID.randomUUID()
        val id = UUID.randomUUID()

        val req = CreateTestReq(
            snippetId = snippetId,
            name = "test-1",
            inputs = listOf("1", "2"),
            expectedOutputs = listOf("3"),
            targetVersionNumber = 1L,
        )

        val snip = snippet(ownerId)
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(snip))

        val saved = TestCase(
            id = id,
            snippetId = snippetId,
            name = "test-1",
            inputs = listOf("1", "2"),
            expectedOutputs = listOf("3"),
            targetVersionNumber = 1L,
        )
        whenever(testCaseRepo.save(any())).thenReturn(saved)

        val dto: TestCaseDto = service.createTestCase(ownerId, req)

        assertEquals(id.toString(), dto.id)
        assertEquals(snippetId, dto.snippetId) // UUID correcto
        assertEquals("test-1", dto.name)
        assertEquals(listOf("1", "2"), dto.inputs)
        assertEquals(listOf("3"), dto.expectedOutputs)
        assertEquals(1L, dto.targetVersionNumber)

        verify(testCaseRepo).save(
            check { tc ->
                assertEquals(snippetId, tc.snippetId)
                assertEquals("test-1", tc.name)
                assertEquals(listOf("1", "2"), tc.inputs)
                assertEquals(listOf("3"), tc.expectedOutputs)
                assertEquals(1L, tc.targetVersionNumber)
            },
        )
    }

    @Test
    fun `listTestCases devuelve lista mapeada correctamente`() {
        val ownerId = "auth0|owner-tests"
        val snippetId = UUID.randomUUID()

        val snip = snippet(ownerId)
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(snip))

        val t1 = TestCase(
            id = UUID.randomUUID(),
            snippetId = snippetId,
            name = "A",
            inputs = listOf("x"),
            expectedOutputs = listOf("y"),
            targetVersionNumber = 2,
        )

        val t2 = TestCase(
            id = UUID.randomUUID(),
            snippetId = snippetId,
            name = "B",
            inputs = emptyList(),
            expectedOutputs = emptyList(),
            targetVersionNumber = null,
        )

        whenever(testCaseRepo.findAllBySnippetId(snippetId)).thenReturn(listOf(t1, t2))

        val list = service.listTestCases(ownerId, snippetId)

        assertEquals(2, list.size)

        val dto1 = list[0]
        assertEquals(t1.id.toString(), dto1.id)
        assertEquals("A", dto1.name)
        assertEquals(listOf("x"), dto1.inputs)
        assertEquals(listOf("y"), dto1.expectedOutputs)
        assertEquals(2L, dto1.targetVersionNumber)

        val dto2 = list[1]
        assertEquals("B", dto2.name)
        assertEquals(emptyList<String>(), dto2.inputs)
        assertEquals(emptyList<String>(), dto2.expectedOutputs)
        assertNull(dto2.targetVersionNumber)

        verify(testCaseRepo).findAllBySnippetId(snippetId)
    }

    @Test
    fun `deleteTestCase carga el test y lo borra`() {
        val ownerId = "auth0|owner-tests"
        val snippetId = UUID.randomUUID()
        val testId = UUID.randomUUID()

        val snip = snippet(ownerId)
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(snip))

        val testCase = TestCase(
            id = testId,
            snippetId = snippetId,
            name = "t",
            inputs = emptyList(),
            expectedOutputs = emptyList(),
            targetVersionNumber = null,
        )
        whenever(testCaseRepo.findById(testId)).thenReturn(Optional.of(testCase))

        service.deleteTestCase(ownerId, testId)

        verify(testCaseRepo).delete(testCase)
    }
}
