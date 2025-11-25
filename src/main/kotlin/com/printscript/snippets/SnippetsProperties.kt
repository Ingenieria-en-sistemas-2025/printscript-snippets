package com.printscript.snippets

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("snippets")
data class SnippetsProperties(
    var assetContainer: String = "snippets",
)
