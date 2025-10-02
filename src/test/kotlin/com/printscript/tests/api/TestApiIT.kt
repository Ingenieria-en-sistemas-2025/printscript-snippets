package com.printscript.tests.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev") // sigue usando tu DevSecurityConfig (permitAll)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TestApiIT {

    companion object {
        @Container
        @JvmStatic
        val pg: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16"):wq

                .withDatabaseName("tests")
                .withUsername("tests")
                .withPassword("tests")

        @JvmStatic
        @DynamicPropertySource
        fun dbProps(reg: DynamicPropertyRegistry) {
            reg.add("spring.datasource.url") { pg.jdbcUrl }
            reg.add("spring.datasource.username") { pg.username }
            reg.add("spring.datasource.password") { pg.password }
            reg.add("spring.flyway.enabled") { true }
            reg.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @LocalServerPort var port: Int = 0
    @Autowired lateinit var rest: TestRestTemplate
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun `crear y ejecutar test end-to-end`() {
        val base = "http://localhost:$port"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            add("X-User-Id", "pepe")
        }

        val createBody = """{"name":"suma feliz","inputs":["3","5"],"expectedOutputs":["8"],"targetVersionNumber":1}"""
        val createResp = rest.postForEntity("$base/snippets/42/tests", HttpEntity(createBody, headers), String::class.java)
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)

        val testId = mapper.readTree(createResp.body).get("id").asLong()
        val runResp = rest.postForEntity("$base/tests/$testId/run", HttpEntity<Void>(headers), String::class.java)
        assertThat(runResp.statusCode).isEqualTo(HttpStatus.OK)
    }
}
