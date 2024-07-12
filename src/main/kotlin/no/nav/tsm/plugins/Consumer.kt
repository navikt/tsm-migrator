package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.HistoriskSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer) {
    launch(Dispatchers.IO) { sykmeldingConsumer.start() }
   // launch(Dispatchers.IO) { historiskSykmeldingConsumer.start() }

//    launch(Dispatchers.IO) { fellesformatConsumer.consumeDump() }
//    launch(Dispatchers.IO) { gamleSykmeldingerConsumer.consumeDump() }
}
