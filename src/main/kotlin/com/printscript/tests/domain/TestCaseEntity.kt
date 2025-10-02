import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "test_case")
data class TestCaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "snippet_id", nullable = false)
    val snippetId: Long,

    @Column(name = "name", length = 120, nullable = false)
    val name: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inputs", columnDefinition = "jsonb", nullable = false)
    var inputs: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_outputs", columnDefinition = "jsonb", nullable = false)
    var expectedOutputs: String?,

    @Column(name = "target_version_number")
    val targetVersionNumber: Long? = null,

    @Column(name = "last_run_status", length = 20, nullable = false)
    var lastRunStatus: String = "NEVER_RUN",

    @Column(name = "last_run_output", columnDefinition = "jsonb")
    var lastRunOutput: String? = null,

    @Column(name = "last_run_at")
    var lastRunAt: Instant? = null,

    @Column(name = "created_by", nullable = false, length = 120)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
