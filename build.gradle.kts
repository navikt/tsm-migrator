
val kotlin_version="2.2.21"
val logback_version= "1.5.21"
val koin_version="4.1.1"
val prometheus_version="0.16.0"
val logback_encoder_version="9.0"
val kafka_version= "3.9.1"
val jackson_version= "2.20.1"
val opentelemetryVersion = "2.22.0"
val mockkVersion = "1.14.7"
val syfoXmlCodegenVersion = "2.0.1"
val jaxbRuntimeVersion = "4.0.6"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaTimeAdapterVersion = "1.1.3"
val sykmelidngInputVersion = "21"
plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
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
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-apache")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("no.nav.helse.xml:sm2013:$syfoXmlCodegenVersion")
    implementation("no.nav.helse.xml:xmlfellesformat:$syfoXmlCodegenVersion")
    implementation("no.nav.helse.xml:kith-hodemelding:$syfoXmlCodegenVersion")
    implementation("no.nav.helse.xml:kith-apprec:$syfoXmlCodegenVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    implementation("com.migesok", "jaxb-java-time-adapters", javaTimeAdapterVersion)
    implementation("no.nav.tsm.sykmelding", "input", sykmelidngInputVersion)
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks {
    shadowJar {
        mergeServiceFiles {

        }
    }
}
