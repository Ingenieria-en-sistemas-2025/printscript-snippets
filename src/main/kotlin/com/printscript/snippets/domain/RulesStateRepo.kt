package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.domain.model.RulesType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RulesStateRepo : JpaRepository<RulesState, Long> {
    fun findByType(type: RulesType): Optional<RulesState>
}
