package com.printscript.snippets.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "snippet",
    uniqueConstraints = [UniqueConstraint(name = "uq_owner_name", columnNames = ["owner_id", "name"])],
)
class Snippet(
    // se usa como parte del naming key en el bucket tipo asi snippets/<snippet_id>/v1.ps
    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @Column(name = "owner_id", nullable = false) // el user id (el sub del token Auth0)
    var ownerId: String,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 40)
    var language: String,

    @Column(nullable = false, length = 10)
    var languageVersion: String,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "current_version_id", columnDefinition = "uuid")
    var currentVersionId: UUID? = null,

    // guarda si la ult version fue validada con exito (tiene la rta de execution)
    @Column(name = "last_is_valid", nullable = false)
    var lastIsValid: Boolean = false,

    // nro de advertencias en la ultima version de lint
    @Column(name = "last_lint_count", nullable = false)
    var lastLintCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    // para setear createdAt/updatedAt sin olvidos
    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun preUpdate() { updatedAt = Instant.now() }
}
