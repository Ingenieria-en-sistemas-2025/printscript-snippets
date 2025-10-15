package com.printscript.tests.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "test_case")
data class TestCaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val snippetId: Long,
    @Column(nullable = false) val name: String,

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    val inputs: List<String>,

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    val expectedOutputs: List<String>,

    @Column(name = "target_version_number")
    val targetVersionNumber: Long? = null,

    val createdBy: String,

    @CreationTimestamp @Column(nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp @Column(nullable = false)
    val updatedAt: Instant? = null,

    val lastRunStatus: String? = null,

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    val lastRunOutput: List<String>? = null,

    val lastRunAt: Instant? = null,
)
