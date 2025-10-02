package com.printscript.tests.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

@Component
class JsonUtils(
    private val mapper: ObjectMapper = jacksonObjectMapper()
) {
    // ---- JSON -> List<String> ----

    // Cuando el valor viene del DB como JsonNode (jsonb mapeado a JsonNode)
    fun jsonToList(node: JsonNode?): List<String>? {
        if (node == null || node.isNull || node.isMissingNode) return null
        return when {
            node.isArray -> node.mapNotNull { it.asText(null) }
            node.isTextual -> mapper.readValue<List<String>>(node.asText())
            else -> listOf(node.asText())
        }
    }

    // Cuando el valor viene como String JSON (columna TEXT o asignación manual)
    fun jsonToList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return mapper.readValue(json)
    }

    // ---- List<String> -> JSON ----

    // Para columnas jsonb mapeadas a JsonNode
    fun listToJsonNode(list: List<String>?): JsonNode? =
        list?.let { mapper.valueToTree(it) }

    // Para columnas String (guardás el JSON como texto)
    fun listToJson(list: List<String>?): String? =
        list?.let { mapper.writeValueAsString(it) }
}
