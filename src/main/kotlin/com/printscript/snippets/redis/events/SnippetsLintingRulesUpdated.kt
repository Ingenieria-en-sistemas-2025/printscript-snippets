package com.printscript.snippets.redis.events

import java.util.UUID

data class SnippetsLintingRulesUpdated(val correlationalId: String, val snippetId: UUID, val language: String, val version: String, val configText: String?, val configFormat: String?, val attempt: Int = 0, val createdAt: Long = System.currentTimeMillis()) : DomainEvent
