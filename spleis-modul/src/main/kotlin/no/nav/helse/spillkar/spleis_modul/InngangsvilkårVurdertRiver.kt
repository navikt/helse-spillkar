package no.nav.helse.spillkar.spleis_modul

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class InngangsvilkårVurdertRiver(
    rapidsConnection: RapidsConnection,
    private val spillkarKlient: SpillkarKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "inngangsvilkår_vurdert")
            }
            validate {
                it.requireKey("@id", "fødselsnummer", "inngangsvilkårsvurderingId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok opplysninger om inngangsvilkår vurdert:\n\t${packet.toJson()}")
        inngangsvilkårVurdert(packet)
    }

    private fun inngangsvilkårVurdert(packet: JsonMessage) = try {
        spillkarKlient.inngangsvilkårVurdert(packet)
    } catch (error: Exception) {
        sikkerlogg.error("Feil ved håndtering av inngangsvilkår vurdert", error)
        throw error
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke inngangsvilkår vurdert:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
