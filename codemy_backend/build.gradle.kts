    plugins {
        alias(libs.plugins.kotlin.jvm)
        alias(libs.plugins.ktor)
        alias(libs.plugins.kotlin.plugin.serialization)
    }

    group = "com.inc.codemy.backend"
    version = "0.0.1"

    application {
        mainClass = "io.ktor.server.netty.EngineMain"
    }

    kotlin {
        jvmToolchain(25)
    }

    dependencies {
        implementation("org.jetbrains.exposed:exposed-core:0.52.0")
        implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
        implementation("com.typesafe:config:1.4.3")  // для ConfigFactory
        implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.52.0")
        implementation("com.typesafe:config:1.4.3")
        implementation("org.mindrot:jbcrypt:0.4")
        implementation("io.ktor:ktor-server-cors-jvm:2.3.8")
        implementation(libs.ktor.server.call.logging)
        implementation(libs.ktor.server.default.headers)
        implementation(libs.ktor.server.core)
        implementation(libs.ktor.server.host.common)
        implementation(libs.ktor.server.status.pages)
        implementation(libs.ktor.server.auth)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.ktor.server.content.negotiation)
        implementation(libs.postgresql)
        implementation(libs.h2)
        implementation(libs.exposed.core)
        implementation(libs.exposed.jdbc)
        implementation(libs.ktor.server.netty)
        implementation(libs.logback.classic)
        implementation(libs.ktor.server.config.yaml)
        testImplementation(libs.ktor.server.test.host)
        testImplementation(libs.kotlin.test.junit)
    }
