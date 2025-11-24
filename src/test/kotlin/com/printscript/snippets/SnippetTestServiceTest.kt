package com.printscript.snippets

import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.service.SnippetTestService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SnippetTestServiceTest {

    @Mock
    lateinit var testCaseRepo: TestCaseRepo

    @InjectMocks
    lateinit var service: SnippetTestService

    @Test
    fun `createTestCase guarda entidad y devuelve DTO`() {
        val id = UUID.randomUUID()
        val snippetId = UUID.randomUUID()

        val req = CreateTestReq(
            snippetId = snippetId.toString(),
            name = "test-1",
            inputs = listOf("1", "2"),
            expectedOutputs = listOf("3"),
            targetVersionNumber = 1L,
        )

        val saved = TestCase(
            id = id,
            snippetId = snippetId,
            name = "test-1",
            inputs = listOf("1", "2"),
            expectedOutputs = listOf("3"),
            targetVersionNumber = 1L,
        )

        whenever(testCaseRepo.save(any())).thenReturn(saved)

        val dto: TestCaseDto = service.createTestCase(req)

        assertEquals(id.toString(), dto.id)
        assertEquals(snippetId.toString(), dto.snippetId)
        assertEquals("test-1", dto.name)
        assertEquals(listOf("1", "2"), dto.inputs)
        assertEquals(listOf("3"), dto.expectedOutputs)
        assertEquals(1L, dto.targetVersionNumber)

        // verificamos params usados al guardar
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
        val snipId = UUID.randomUUID()

        val t1 = TestCase(
            id = UUID.randomUUID(),
            snippetId = snipId,
            name = "A",
            inputs = listOf("x"),
            expectedOutputs = listOf("y"),
            targetVersionNumber = 2,
        )

        val t2 = TestCase(
            id = UUID.randomUUID(),
            snippetId = snipId,
            name = "B",
            inputs = emptyList(),
            expectedOutputs = emptyList(),
            targetVersionNumber = null,
        )

        whenever(testCaseRepo.findAllBySnippetId(snipId)).thenReturn(listOf(t1, t2))

        val list = service.listTestCases(snipId)

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

        verify(testCaseRepo).findAllBySnippetId(snipId)
    }

    @Test
    fun `deleteTestCase llama a repo deleteById`() {
        val id = UUID.randomUUID()

        service.deleteTestCase(id)

        verify(testCaseRepo).deleteById(id)
    }
}
