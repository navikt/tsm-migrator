package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.digital.DigitalSykmeldingConsumer
import no.nav.tsm.reformat.sykmelding.SykmeldingReformatService
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer,
                                  sykmeldingReformatService: SykmeldingReformatService,
                                  digitalSykmeldingConsumer: DigitalSykmeldingConsumer) {
    val sykmeldingConsumerJob = launch(Dispatchers.IO) { sykmeldingConsumer.start() }
    val sykmeldingReformatJob = launch(Dispatchers.IO) { sykmeldingReformatService.start() }
    //TODO("enable this") val digitalSykmeldingConsumer = launch(Dispatchers.IO) { digitalSykmeldingConsumer.start() }
    environment.monitor.subscribe(ApplicationStopping) {
        sykmeldingReformatJob.cancel()
        sykmeldingConsumerJob.cancel()
        //TODO("enable this") //digitalSykmeldingConsumer.cancel()
    }
}
