package com.printscript.tests.domain

import com.printscript.tests.domain.model.SnippetVersion
import org.springframework.data.jpa.repository.JpaRepository

interface SnippetVersionRepo: JpaRepository<SnippetVersion, Long> {
    //busca la ult version de un snippet osoea la de nro mas alto
    fun findTopBySnippetIdOrderByVersionNumberDesc(snippetId: Long): SnippetVersion?

}