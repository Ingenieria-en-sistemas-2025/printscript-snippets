package com.printscript.snippets.service.rules

enum class FormatRuleId(val id: String) {
    // Boolean rules
    SPACING_AROUND_EQUALS("enforce-spacing-around-equals"),
    NO_SPACING_AROUND_EQUALS("enforce-no-spacing-around-equals"),
    SPACE_AFTER_COLON_IN_DECL("enforce-spacing-after-colon-in-declaration"),
    SPACE_BEFORE_COLON_IN_DECL("enforce-spacing-before-colon-in-declaration"),
    INDENT_INSIDE_IF("indent-inside-if"),
    MANDATORY_SINGLE_SPACE_SEPARATION("mandatory-single-space-separation"),
    IF_BRACE_BELOW_LINE("if-brace-below-line"),
    IF_BRACE_SAME_LINE("if-brace-same-line"),

    // Numeric rules
    INDENT_SIZE("indent_size"),
    INDENT_SPACES("indent-spaces"),
    TABSIZE("tabsize"),
    LINE_BREAKS_AFTER_PRINTLN("line-breaks-after-println"),
    ;

    companion object {
        private val BY_ID = entries.associateBy { it.id }

        fun fromId(id: String): FormatRuleId? = BY_ID[id]
    }
}
