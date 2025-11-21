package com.printscript.snippets.domain.model

import com.printscript.snippets.enums.RulesType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(
    name = "rules_state",
    uniqueConstraints = [
        // una fila por (tipo, owner) — ownerId null = GLOBAL
        UniqueConstraint(name = "uq_rules_scope", columnNames = ["type", "owner_id"]),
    ],
)
class RulesState(
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: RulesType,

    @Column(name = "owner_id", length = 64)
    var ownerId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_json", columnDefinition = "jsonb", nullable = false)
    var enabledJson: List<String> = emptyList(), // ids de reglas habilitadas

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    var optionsJson: Map<String, Any?>? = null, // indent numb y esas cosas

    @Column(name = "config_text", columnDefinition = "text")
    var configText: String? = null, // el file de config de las reglas

    @Column(name = "config_format", length = 32)
    var configFormat: String? = null, // el formato del config file
)
