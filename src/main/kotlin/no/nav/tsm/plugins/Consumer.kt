package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer
import no.nav.tsm.sykmeldinger.kafka.FellesformatConsumer

fun Application.configureConsumer(dumpConsumer: DumpConsumer, fellesformatConsumer: FellesformatConsumer) {
    launch(Dispatchers.IO) { dumpConsumer.consumeDump() }
    launch(Dispatchers.IO) { fellesformatConsumer.consumeDump() }
}
