package com.printscript.tests.execution

import com.printscript.tests.execution.dto.FormatReq
import com.printscript.tests.execution.dto.FormatRes
import com.printscript.tests.execution.dto.LintReq
import com.printscript.tests.execution.dto.LintRes
import com.printscript.tests.execution.dto.ParseReq
import com.printscript.tests.execution.dto.ParseRes
import com.printscript.tests.execution.dto.RunSingleTestReq
import com.printscript.tests.execution.dto.RunSingleTestRes

interface SnippetExecution {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun format(req: FormatReq): FormatRes

    // fun run(req: RunReq): RunRes
    fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes
}
