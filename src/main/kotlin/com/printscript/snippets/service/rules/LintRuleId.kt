package com.printscript.snippets.service.rules

enum class LintRuleId(val id: String) {
    IDENTIFIER_STYLE("IdentifierStyleRuleStreaming"),
    PRINTLN_SIMPLE_ARG("PrintlnSimpleArgRuleStreaming"),
    READ_INPUT_SIMPLE_ARG("ReadInputSimpleArgRuleStreaming"),
    ;

    companion object {
        private val BY_ID = entries.associateBy { it.id }

        fun fromId(id: String): LintRuleId? = BY_ID[id]
    }
}
