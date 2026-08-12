val kotlin = "2.4.10"
val logback = "1.5.34"
val logback_encoder = "9.0"
val kafka = "3.9.1"
val mockk = "1.13.12"
val syfoXmlCodegen = "2.0.1"
val jaxbRuntime = "2.4.0-b180830.0438"
val jaxbApi = "2.4.0-b180830.0359"
val javaTimeAdapter = "1.1.3"
val sykmeldingInput = "29"

plugins {
    kotlin("jvm") version "2.4.10"
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
    implementation(tsmKtorLibs.kafka.sykmeldinger)
    implementation("org.apache.kafka:kafka-clients:$kafka")
    implementation("net.logstash.logback:logstash-logback-encoder:${logback_encoder}")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("no.nav.helse.xml:sm2013:$syfoXmlCodegen")
    implementation("no.nav.helse.xml:xmlfellesformat:$syfoXmlCodegen")
    implementation("no.nav.helse.xml:kith-hodemelding:$syfoXmlCodegen")
    implementation("no.nav.helse.xml:kith-apprec:$syfoXmlCodegen")
    implementation("javax.xml.bind:jaxb-api:$jaxbApi")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntime")
    implementation("com.migesok:jaxb-java-time-adapters:$javaTimeAdapter")
    implementation("no.nav.tsm.sykmelding:input:$sykmeldingInput")
    testImplementation(ktorLibs.server.testHost)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin")
    testImplementation("io.mockk:mockk:$mockk")
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
