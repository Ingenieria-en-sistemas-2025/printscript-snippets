package com.printscript.snippets.execution

import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes
import com.printscript.snippets.execution.dto.RunReq
import com.printscript.snippets.execution.dto.RunRes
import com.printscript.snippets.execution.dto.RunSingleTestReq
import com.printscript.snippets.execution.dto.RunSingleTestRes

interface SnippetExecution {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun format(req: FormatReq): FormatRes
    fun run(req: RunReq): RunRes
    fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes
}
