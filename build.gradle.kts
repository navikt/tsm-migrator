
val kotlin_version="2.0.0"
val logback_version="1.5.6"
val koin_version="3.5.6"
val prometheus_version="0.16.0"
val logback_encoder_version="7.4"
val kafka_version="3.7.0"
val jackson_version= "2.17.2"
val opentelemetryVersion = "2.5.0"
val mockkVersion = "1.13.11"
plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

group = "no.nav.tsm"
version = "0.0.1"

application {
    mainClass.set("no.nav.tsm.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("net.logstash.logback:logstash-logback-encoder:${logback_encoder_version}")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.prometheus:simpleclient_hotspot:$prometheus_version")
    implementation("io.prometheus:simpleclient_common:$prometheus_version")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:$opentelemetryVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockkVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
}

tasks {
    shadowJar {
        mergeServiceFiles {

        }
    }
}
