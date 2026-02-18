package no.nav.helse.spillkar.spleis_modul

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class InngangsvilkårsvurderingsløserRiver(
    rapidsConnection: RapidsConnection,
    private val spillkarKlient: SpillkarKlient
) : River.PacketListener {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireAll("@behov", listOf("Inngangsvilkårsvurdering")) } // TODO _må_ dette være en liste?
            precondition { it.forbid("@løsning") }
            validate { it.requireKey("@id", "@opprettet", "fødselsnummer", "skjæringstidspunkt") }
        }.register(this)
    }


    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok behov for inngangsvilkårsvurdering:\n\t${packet.toJson()}")
        val fødselsnummer = packet["fødselsnummer"].asText()

        val inngangsvilkårvurdering = inngangsvilkårsvurderingOrNull(packet) ?: return

        packet["@løsning"] = mapOf(
            "Inngangsvilkårsvurdering" to inngangsvilkårvurdering // TODO blir dette riktig?
        )

        val json = packet.toJson()
        context.publish(fødselsnummer, json).also {
            sikkerlogg.info("Sender løsning for inngangsvilkårsvurdering:\n\t${json}")
        }
    }

    private fun inngangsvilkårsvurderingOrNull(packet: JsonMessage) = try {
        spillkarKlient.inngangsvilkårsvurdering(packet)
    } catch (error: Exception) {
        sikkerlogg.error("Feil ved håndtering av inngangsvilkårvurdering", error)
        throw error
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke inngangsvilkår vurdert:\n${problems.toExtendedReport()}")
    }
}
