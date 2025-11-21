package com.printscript.snippets.service

import com.printscript.snippets.domain.model.enums.Compliance
import com.printscript.snippets.domain.model.enums.LintStatus

object ComplianceCalculator {
    fun compute(lintStatus: LintStatus, isValid: Boolean, lintCount: Int): Compliance =
        when (lintStatus) {
            LintStatus.FAILED -> Compliance.FAILED
            LintStatus.PENDING,
            LintStatus.RUNNING,
            -> Compliance.PENDING
            LintStatus.DONE -> {
                if (!isValid) {
                    Compliance.NOT_COMPLIANT
                } else if (lintCount > 0) {
                    Compliance.NOT_COMPLIANT
                } else {
                    Compliance.COMPLIANT
                }
            }
        }
}
