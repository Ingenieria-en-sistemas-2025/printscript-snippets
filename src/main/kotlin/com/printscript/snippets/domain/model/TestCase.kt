package com.printscript.snippets.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "test_case",
)
class TestCase(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // FK
    @Column(name = "snippet_id", nullable = false)
    var snippetId: Long,

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
