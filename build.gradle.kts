plugins {
    kotlin("jvm") version "1.9.23" // proy en kt corre en jvm
    kotlin("plugin.spring") version "1.9.23" // hace q kt genere clases compatibles con spring
    kotlin("plugin.jpa") version "1.9.23"
    id("org.springframework.boot") version "3.5.6" // facilita arrancar la app
    id("io.spring.dependency-management") version "1.1.7" // maneja las vers de dependencias de spring
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "com.printscript"
version = "0.0.1-SNAPSHOT"
description = "PrintScript Snippets microservice"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/austral-ingsis/class-redis-streams")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Ingenieria-en-sistemas-2025/printscript-contracts")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.4") // para conectarse con pg
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") // da hibernate, jpa, etc
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core") // para q spring maneje migraciones
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect") // Hibernate necesita reflection para leer mis clases
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // para validacion jwt y eso
    implementation("com.newrelic.agent.java:newrelic-api:8.10.0")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:postgresql")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    testImplementation("org.testcontainers:junit-jupiter")

    testImplementation("org.springframework.boot:spring-boot-starter-test") // JUnit5 + Mockito + MockMvc
    testImplementation("io.mockk:mockk:1.13.12") // mocking para Kotlin

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.austral.ingsis:redis-streams-mvc:0.1.13")

    implementation("io.printscript:contracts:0.1.5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
}

// configura el compilador de kt
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

spotless {
    kotlin {
        target("**/*.kt") // archivos a formatear
        ktlint("0.50.0").editorConfigOverride(
            mapOf("max_line_length" to "400", "indent_size" to "4"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("0.50.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config = files("$rootDir/config/detekt/detekt.yml")
}

jacoco { toolVersion = "0.8.10" }

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // 80%
            }
        }
    }
}

tasks.check {
    dependsOn("detekt", "spotlessCheck", tasks.jacocoTestCoverageVerification)
}

// Git hooks
val gitDir = layout.projectDirectory.dir(".git")
val hooksSrc = layout.projectDirectory.dir("hooks")
val hooksDst = layout.projectDirectory.dir(".git/hooks")

tasks.register<Copy>("installGitHooks") {
    onlyIf { gitDir.asFile.exists() && hooksSrc.asFile.exists() }
    from(hooksSrc)
    into(hooksDst)
    fileMode = Integer.parseInt("775", 8) // chmod +x
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register("ensureGitHooks") {
    dependsOn("installGitHooks")
    onlyIf { gitDir.asFile.exists() }
}
