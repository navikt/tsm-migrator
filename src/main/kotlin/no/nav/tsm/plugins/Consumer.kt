package no.nav.tsm.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer, migrertSykmeldingConsumer: MigrertSykmeldingConsumer) {
    launch(Dispatchers.IO) { sykmeldingConsumer.start() }
    launch(Dispatchers.IO) { migrertSykmeldingConsumer.start() }
}
