package com.printscript.tests.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "snippet_version",
    uniqueConstraints = [UniqueConstraint(name = "uq_sn_ver", columnNames = ["snippet_id", "version_number"])]
)
class SnippetVersion(
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "snippet_id", nullable = false, columnDefinition = "uuid")
    var snippetId: UUID,

    @Column(name = "version_number", nullable = false)
    var versionNumber: Long,

    @Column(name = "content_key", nullable = false, length = 512)
    var contentKey: String,

    @Column(name = "formatted_key", length = 512)
    var formattedKey: String? = null,

    @Column(name = "is_formatted", nullable = false) var isFormatted: Boolean = false,
    @Column(name = "is_valid",     nullable = false) var isValid: Boolean = false,
    @Column(name = "lint_issues",  columnDefinition = "JSONB") var lintIssues: String = "[]",
    @Column(name = "parse_errors", columnDefinition = "JSONB") var parseErrors: String = "[]",
    @Column(name = "created_at",   nullable = false) var createdAt: Instant = Instant.now()
)
