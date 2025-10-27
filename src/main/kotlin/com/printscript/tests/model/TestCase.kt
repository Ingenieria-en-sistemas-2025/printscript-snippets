package com.printscript.tests.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "test_case"
)
class TestCase(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "snippet_id", nullable = false)
    var snippetId: Long,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(name = "inputs", columnDefinition = "jsonb", nullable = false)
    var inputs: String = "[]",

    @Column(name = "expected_outputs", columnDefinition = "jsonb", nullable = false)
    var expectedOutputs: String = "[]",

    @Column(name = "target_version_number")
    var targetVersionNumber: Long? = null,

    @Column(name = "created_by", nullable = false)
    var createdBy: String,

    @Column(name="created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name="updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)