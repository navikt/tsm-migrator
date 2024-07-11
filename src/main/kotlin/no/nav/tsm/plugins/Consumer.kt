package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer
import no.nav.tsm.sykmeldinger.kafka.FellesformatConsumer
import no.nav.tsm.sykmeldinger.kafka.GamleSykmeldingerConsumer
import no.nav.tsm.sykmeldinger.kafka.HistoriskSykmeldingConsumer

fun Application.configureConsumer(historiskSykmeldingConsumer: HistoriskSykmeldingConsumer) {
    //launch(Dispatchers.IO) { dumpConsumer.consumeDump() }
    launch(Dispatchers.IO) { historiskSykmeldingConsumer.start() }

//    launch(Dispatchers.IO) { fellesformatConsumer.consumeDump() }
//    launch(Dispatchers.IO) { gamleSykmeldingerConsumer.consumeDump() }
}
