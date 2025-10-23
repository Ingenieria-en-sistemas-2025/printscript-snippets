package com.printscript.tests.config

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
                    // 1. ENDPOINTS DE SNIPPETS
                    // GET /snippets/all (Listar)
                    .requestMatchers(GET, "/snippets/all").hasAuthority("SCOPE_read:snippets")
                    // POST /snippets/create (Crear)
                    .requestMatchers(POST, "/snippets/create").hasAuthority("SCOPE_write:snippets")
                    // GET /snippets/{id} (Obtener por ID)
                    .requestMatchers(GET, "/snippets/*").hasAuthority("SCOPE_read:snippets")
                    // PUT /snippets/{id} (Actualizar por ID)
                    .requestMatchers(PUT, "/snippets/*").hasAuthority("SCOPE_write:snippets")
                    // DELETE /snippets/{id} (Eliminar por ID)
                    .requestMatchers(DELETE, "/snippets/*").hasAuthority("SCOPE_write:snippets")
                    // POST /snippets/share (Compartir)
                    .requestMatchers(POST, "/snippets/share").hasAuthority("SCOPE_write:snippets")
                    // GET /snippets/users (Listar Usuarios/Amigos)
                    .requestMatchers(GET, "/snippets/users").hasAuthority("SCOPE_read:users")

                    // 2. ENDPOINTS DE REGLAS (RULES) Y CONFIG
                    // GET /snippets/rules/{ruleType} (Obtener reglas)
                    .requestMatchers(GET, "/snippets/rules/*").hasAuthority("SCOPE_read:rules")
                    // PUT /snippets/rules (Modificar reglas)
                    .requestMatchers(PUT, "/snippets/rules").hasAuthority("SCOPE_write:rules")
                    // GET /snippets/config/filetypes (Listar Tipos Archivo)
                    .requestMatchers(GET, "/snippets/config/filetypes").hasAuthority("SCOPE_read:config")

                    // 3. ENDPOINTS DE CASOS DE PRUEBA (CASES) y RUN (EJECUCIÓN)
                    // GET/POST/DELETE para Cases
                    .requestMatchers(GET, "/snippets/cases/*").hasAuthority("SCOPE_read:test-cases")
                    .requestMatchers(POST, "/snippets/cases").hasAuthority("SCOPE_write:test-cases")
                    .requestMatchers(DELETE, "/snippets/cases/*").hasAuthority("SCOPE_write:test-cases")

                    // POST /snippets/run/format (Formato) y POST /snippets/run/case/{testCaseId} (Testear)
                    .requestMatchers(POST, "/snippets/run/*").hasAuthority("SCOPE_execute:code")

                    // Fallback: Cualquier otra ruta requiere autenticacion, pero sin scope especifico
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(permissionsConverter()) }
            }
            .csrf { it.disable() }
            .cors { } // Habilita el manejo de CORS. Spring Security buscará un CorsConfigurationSource bean.
            .build()

    // SOLUCION PARA EL ERROR DE RESTCLIENT (Bean definition)
    // despues veo si hay una mejor forma de solucionarlo
    @Bean
    fun restClient(): RestClient {
        // Le dice a Spring que registre esta instancia para inyeccion (@Autowired)
        return RestClient.create()
    }

    // CORS
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // Despues cambiar "*" al dominio exacto de la UI ("http://localhost:5173")
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration) // Aplica la configuracion a todas las rutas
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