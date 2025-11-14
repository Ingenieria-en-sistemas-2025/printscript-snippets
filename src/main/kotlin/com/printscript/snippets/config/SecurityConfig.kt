package com.printscript.snippets.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.RestClient
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@Profile("!test")
class SecurityConfig(
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuer: String,
    @param:Value("\${auth0.audience}")
    private val audience: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .authorizeHttpRequests {
                it
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    // 1. ENDPOINTS DE SNIPPETS (CRUD)
                    // Lectura de snippets y sus tests (read:snippets)
                    .requestMatchers("/internal/**").authenticated()
                    .requestMatchers(POST, "/snippets/share").authenticated()
                    .requestMatchers(GET, "/snippets/all").authenticated()
                    .requestMatchers(GET, "/snippets/*").authenticated()
                    .requestMatchers(GET, "/snippets/cases/*").authenticated()
                    // Escritura/Modificación/Eliminación de snippets y sus tests (write:snippets)
                    .requestMatchers(POST, "/snippets").authenticated()
                    .requestMatchers(PUT, "/snippets/*").authenticated()
                    .requestMatchers(DELETE, "/snippets/*").authenticated()
                    .requestMatchers(POST, "/snippets/cases").authenticated()
                    .requestMatchers(DELETE, "/snippets/cases/*").authenticated()
                    // GET /snippets/users -> Listar Usuarios/Amigos (read:users)
                    .requestMatchers(GET, "/api/users").authenticated()
                    // 2. ENDPOINTS DE REGLAS (RULES) Y CONFIG
                    // Administracion de Reglas/Config (admin:rules)
                    .requestMatchers(GET, "/snippets/rules/*").authenticated()
                    .requestMatchers(PUT, "/snippets/rules").authenticated()
                    .requestMatchers(GET, "/snippets/config/filetypes").authenticated()
                    // --- DESCARGA DE SNIPPET ---
                    .requestMatchers(GET, "/snippets/*/download").authenticated()
// --- SNIPPET DESDE ARCHIVO (multipart) ---
                    .requestMatchers(POST, "/snippets/file").authenticated()
                    .requestMatchers(PUT, "/snippets/*/file").authenticated()
// --- TEST CASES ---
                    .requestMatchers(GET, "/snippets/*/tests").authenticated()
                    .requestMatchers(POST, "/snippets/*/tests").authenticated()
                    .requestMatchers(DELETE, "/snippets/tests/*").authenticated()
// --- RUN DE TESTS ---
                    .requestMatchers(POST, "/snippets/*/tests/*/run").authenticated()
                    // 3. ENDPOINTS DE EJECUCIÓN (RUN)
                    // POST /snippets/run/* (Formato, Testear, etc.) -> execute:code
                    .requestMatchers(POST, "/snippets/run/*").authenticated()
                    // Fallback: Cualquier otra ruta requiere autenticacion, pero sin scope especifico
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(permissionsConverter()) }
            }
            .csrf { it.disable() }
            .cors { }
            .build()

    // SOLUCION PARA EL ERROR DE RESTCLIENT (Bean definition)
    // despues veo si hay una mejor forma de solucionarlo
    @Bean
    fun restClient(): RestClient {
        return RestClient.builder()
            .requestInterceptor { request, body, execution ->
                // Busca un correlation-id actual (si no hay, crea uno nuevo)
                val id = org.slf4j.MDC.get("correlation-id")
                    ?: java.util.UUID.randomUUID().toString()

                // Lo agrega como header en el request
                request.headers.add("X-Correlation-Id", id)

                // Ejecuta el request
                execution.execute(request, body)
            }
            .build()
    }

    @Bean
    fun plainRestClient(): RestClient = RestClient.create()

    // CORS
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val c = CorsConfiguration()
        c.allowedOrigins = listOf(
            "http://localhost:5173",
            "https://printscript-prod.duckdns.org",
            "https://printscript-dev.duckdns.org",
        )
        c.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        c.allowedHeaders = listOf("*")
        c.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", c)
        return source
    }

    // Funcion para extraer los scopes de Auth0
    private fun permissionsConverter(): Converter<Jwt, AbstractAuthenticationToken> {
        val base = JwtGrantedAuthoritiesConverter().apply { setAuthorityPrefix("SCOPE_") }
        return Converter { jwt ->
            val authorities = base.convert(jwt)?.toMutableSet()
            // claims del token
            val perms = jwt.getClaimAsStringList("permissions") ?: emptyList()
            authorities?.addAll(perms.map { SimpleGrantedAuthority("SCOPE_$it") })

            // Crea un token que usa los scopes como Authorities
            JwtAuthenticationToken(jwt, authorities, jwt.subject)
        }
    }

    // Bean del decodificador JWT (valida la firma del token)
    @Bean
    fun jwtDecoder(tokenValidator: OAuth2TokenValidator<Jwt>): JwtDecoder =
        NimbusJwtDecoder.withIssuerLocation(issuer).build().apply {
            setJwtValidator(tokenValidator)
        }

    // Chequea que el token es para este servicio (Audience)
    @Bean
    fun tokenValidator(): OAuth2TokenValidator<Jwt> {
        // 1) Validar por issuer y checks estandar (exp, nbf, etc.)
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)

        // 2) Validar audience
        val audienceClaimValidator = JwtClaimValidator<List<String>>("aud") { audList ->
            audList != null && audience in audList
        }
        // ambas validaciones
        return DelegatingOAuth2TokenValidator(withIssuer, audienceClaimValidator)
    }
}
