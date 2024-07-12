package no.nav.tsm

import configureDatabases
import configureDependencyInjection
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import no.nav.tsm.plugins.configureMonitoring
import no.nav.tsm.plugins.configureRouting
import no.nav.tsm.plugins.configureSerialization

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configureSerialization()
    configureRouting()
    configureMonitoring()
    configureDatabases()
    //configureConsumer(get())
}
