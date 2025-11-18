package com.printscript.snippets.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "language_config",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["language", "version"]),
    ],
)
data class LanguageConfig(

    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID? = null,

    @Column(nullable = false)
    val language: String,

    @Column(nullable = false)
    val extension: String,

    @Column(nullable = false)
    val version: String,
)
