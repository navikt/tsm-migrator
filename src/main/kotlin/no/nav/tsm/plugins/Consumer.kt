package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer
import no.nav.tsm.sykmeldinger.kafka.FellesformatConsumer
import no.nav.tsm.sykmeldinger.kafka.GamleSykmeldingerConsumer

fun Application.configureConsumer(fellesformatConsumer: FellesformatConsumer) {
//    launch(Dispatchers.IO) { dumpConsumer.consumeDump() }
    launch(Dispatchers.IO) { fellesformatConsumer.consumeDump() }
//    launch(Dispatchers.IO) { gamleSykmeldingerConsumer.consumeDump() }
}
