package com.printscript.snippets.redis.events

import com.printscript.snippets.execution.dto.FormatterOptionsDto

data class SnippetsFormattingRulesUpdated(val correlationalId: String, val snippetId: Long, val language: String, val version: String, val configText: String?, val configFormat: String?, val options: FormatterOptionsDto?, val attempt: Int = 0, val createdAt: Long = System.currentTimeMillis()) : DomainEvent
