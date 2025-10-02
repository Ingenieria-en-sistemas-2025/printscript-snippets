package com.printscript.tests.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "test_run")
class TestRunEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "test_id", nullable = false)
    var testId: Long,

    @Column(name = "snippet_id", nullable = false)
    var snippetId: Long,

    @Column(name = "snippet_version_number", nullable = false)
    var snippetVersionNumber: Long,

    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "inputs", nullable = false, columnDefinition = "jsonb")
    var inputsJson: String?,

    @Column(name = "expected_outputs", nullable = false, columnDefinition = "jsonb")
    var expectedOutputsJson: String?,

    @Column(name = "outputs", columnDefinition = "jsonb")
    var outputsJson: String? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "duration_ms")
    var durationMs: Long? = null,

    @Column(name = "executed_by", length = 120)
    var executedBy: String? = null,

    @Column(name = "executed_at", nullable = false)
    var executedAt: Instant = Instant.now()
)
