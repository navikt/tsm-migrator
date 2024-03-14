package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer

fun Application.configureConsumer(sykmeldingConsumer: DumpConsumer) {
    launch(Dispatchers.IO) { sykmeldingConsumer.consumeDump() }
}
