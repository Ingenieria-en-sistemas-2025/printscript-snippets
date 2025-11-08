package com.printscript.snippets.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "rules_state")
class RulesState(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    var type: RulesType, // FORMAT | LINT

    @Column(name = "enabled_json", nullable = false, columnDefinition = "text")
    var enabledJson: String, // JSON array de ids habilitados

    @Column(name = "options_json", columnDefinition = "text")
    var optionsJson: String? = null, // JSON con parámetros (indentSpaces, maxLineLength, etc.)

    @Column(name = "config_text", columnDefinition = "text")
    var configText: String? = null,

    @Column(name = "config_format", length = 32)
    var configFormat: String? = null,
)

enum class RulesType { FORMAT, LINT }
