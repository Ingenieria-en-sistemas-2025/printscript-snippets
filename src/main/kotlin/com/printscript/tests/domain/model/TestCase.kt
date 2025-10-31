package com.printscript.tests.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "test_case",
)
class TestCase(
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    // FK
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "snippet_id", nullable = false, columnDefinition = "uuid")
    var snippetId: UUID,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(name = "inputs", columnDefinition = "jsonb", nullable = false)
    var inputs: String = "[]",

    @Column(name = "expected_outputs", columnDefinition = "jsonb", nullable = false)
    var expectedOutputs: String = "[]",

    // la version correspondiente al snippet que aplica este test case
    @Column(name = "target_version_number")
    var targetVersionNumber: Long? = null,

    @Column(name = "created_by", nullable = false)
    var createdBy: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

// 1 snippet tiene 1 o muchos test cases
