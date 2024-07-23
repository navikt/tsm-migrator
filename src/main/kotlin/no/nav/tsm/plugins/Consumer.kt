package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer, migrertSykmeldingConsumer: MigrertSykmeldingConsumer) {
    val sykmeldingConsumerJob = launch(Dispatchers.IO) { sykmeldingConsumer.start() }
    val migrertSykmeldingConsumerJob = launch(Dispatchers.IO) { migrertSykmeldingConsumer.start() }

    environment.monitor.subscribe(ApplicationStopping) {
        sykmeldingConsumerJob.cancel()
        migrertSykmeldingConsumerJob.cancel()
    }
}
