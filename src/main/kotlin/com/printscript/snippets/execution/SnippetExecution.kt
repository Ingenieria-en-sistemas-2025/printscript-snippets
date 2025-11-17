package com.printscript.snippets.execution

import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes

interface SnippetExecution {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun format(req: FormatReq): FormatRes
    fun run(req: RunReq): RunRes
    fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes
}
