package com.printscript.snippets.redis.controllers

import com.printscript.snippets.execution.dto.FormatterOptionsDto
import com.printscript.snippets.redis.service.BulkRulesService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets/rules")
class RulesController(private val bulk: BulkRulesService) {
    @PutMapping("/formatting")
    fun updateFmt(@RequestBody b: UpdateFmtRulesReq) = bulk
        .onFormattingRulesChanged(b.configText, b.configFormat, b.options)
        .let { ResponseEntity.accepted().build<Void>() }

    @PutMapping("/linting")
    fun updateLint(@RequestBody b: UpdateLintRulesReq) = bulk
        .onLintingRulesChanged(b.configText, b.configFormat)
        .let { ResponseEntity.accepted().build<Void>() }
}

data class UpdateFmtRulesReq(val configText: String?, val configFormat: String?, val options: FormatterOptionsDto?)
data class UpdateLintRulesReq(val configText: String?, val configFormat: String?)
