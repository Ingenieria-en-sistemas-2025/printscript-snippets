package com.printscript.tests.error

data class ApiDiagnostic(val ruleId: String, val message: String, val line: Int, val col: Int)
