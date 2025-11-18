package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.LanguageConfig
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LanguageConfigRepo : JpaRepository<LanguageConfig, UUID> {

    fun findByLanguage(language: String): List<LanguageConfig>
    fun findAllByOrderByLanguageAscVersionDesc(): List<LanguageConfig>
}