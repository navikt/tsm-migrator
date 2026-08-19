package no.nav.tsm

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import no.nav.tsm.migrator.configureMigrator
import no.nav.tsm.plugins.configureDependencyInjection
import no.nav.tsm.migrator.configureKafkaDependencies
import no.nav.tsm.plugins.configureMonitoring

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configureMonitoring()

    configureKafkaDependencies()
    configureMigrator()
}
