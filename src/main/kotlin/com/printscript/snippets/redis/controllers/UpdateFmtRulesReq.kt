package com.printscript.snippets.redis.controllers

import com.printscript.snippets.execution.dto.FormatterOptionsDto

data class UpdateFmtRulesReq(val configText: String?, val configFormat: String?, val options: FormatterOptionsDto?)
