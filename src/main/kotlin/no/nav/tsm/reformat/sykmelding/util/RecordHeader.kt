package no.nav.tsm.reformat.sykmelding.util

import no.nav.tsm.smregister.models.ReceivedSykmelding
import org.apache.kafka.clients.consumer.ConsumerRecord

fun recordContainSyfosmmanuellHeader(record: ConsumerRecord<String, ReceivedSykmelding>): Boolean {
    val headers = record.headers()
    for (header in headers) {
        if (header.key() == "source") {
            val headerValue = String(header.value())
            if (headerValue == "syfosmmanuell-backend") {
                return true
            }
        }
    }
    return false
}