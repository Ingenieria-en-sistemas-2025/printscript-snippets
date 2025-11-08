package com.printscript.snippets.redis.events

import com.printscript.snippets.execution.dto.FormatterOptionsDto
import java.util.UUID

data class SnippetsFormattingRulesUpdated(val correlationalId: String, val snippetId: UUID, val language: String, val version: String, val configText: String?, val configFormat: String?, val options: FormatterOptionsDto? = null, val attempt: Int = 0, val createdAt: Long = System.currentTimeMillis()) : DomainEvent
