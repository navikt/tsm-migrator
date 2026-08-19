plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

group = "no.nav.tsm"
version = "0.0.1"

application {
    mainClass.set("no.nav.tsm.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.serialization.jackson3)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.client.apache5)
    implementation(ktorLibs.server.netty)
    implementation(tsmKtorLibs.core)
    implementation(tsmKtorLibs.kafka)
    implementation(tsmKtorLibs.kafka.sykmeldinger)
    implementation(libs.kafka.clients)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.logback.classic)
    implementation(libs.prometheus.simpleclient.hotspot)
    implementation(libs.prometheus.simpleclient.common)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.syfo.xml.codegen.sm2013)
    implementation(libs.syfo.xml.codegen.xmlfellesformat)
    implementation(libs.syfo.xml.codegen.kith.hodemelding)
    implementation(libs.syfo.xml.codegen.kith.apprec)
    implementation(libs.jaxb.api)
    implementation(libs.jaxb.runtime)
    implementation(libs.jaxb.java.time.adapters)
    implementation(libs.tsm.sykmeldinger.input)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
        from("src/main/resources/logback.xml") {
            into("/")
        }
    }
}
