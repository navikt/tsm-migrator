package no.nav.tsm.sykmelding.util

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.GregorianCalendar
import javax.xml.bind.DatatypeConverter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
val fellesformatJaxBContext: JAXBContext =
    JAXBContext.newInstance(
        XMLEIFellesformat::class.java,
        XMLMsgHead::class.java,
        HelseOpplysningerArbeidsuforhet::class.java
    )

val fellesformatUnmarshaller: Unmarshaller =
    fellesformatJaxBContext.createUnmarshaller().apply {
        setAdapter(LocalDateTimeXmlAdapter::class.java, XMLDateTimeAdapter())
        setAdapter(LocalDateXmlAdapter::class.java, XMLDateAdapter())
    }

fun safeUnmarshal(inputMessageText: String): XMLEIFellesformat {
    // Disable XXE
    val spf: SAXParserFactory = SAXParserFactory.newInstance()
    spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    spf.isNamespaceAware = true

    val xmlSource: Source =
        SAXSource(
            spf.newSAXParser().xmlReader,
            InputSource(StringReader(inputMessageText)),
        )
    return fellesformatUnmarshaller.unmarshal(xmlSource) as XMLEIFellesformat
}


class XMLDateTimeAdapter : LocalDateTimeXmlAdapter() {
    override fun unmarshal(stringValue: String?): LocalDateTime? =
        when (stringValue) {
            null -> null
            else ->
                (DatatypeConverter.parseDateTime(stringValue) as GregorianCalendar)
                    .toZonedDateTime()
                    .toLocalDateTime()
        }
}

class XMLDateAdapter : LocalDateXmlAdapter() {
    override fun unmarshal(stringValue: String?): LocalDate? =
        when (stringValue) {
            null -> null
            else ->
                DatatypeConverter.parseDate(stringValue)
                    .toInstant()
                    .atZone(ZoneOffset.MAX)
                    .toLocalDate()
        }
}
