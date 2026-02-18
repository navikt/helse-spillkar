package no.nav.helse.spillkar.api.instrumentkasse

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.*

data class InngangsvilkårVurdert(val inngangsvilkårVurdertId: UUID) {

    companion object {
        fun opprett(json: ObjectNode): InngangsvilkårVurdert {
            return InngangsvilkårVurdert(
                inngangsvilkårVurdertId = UUID.fromString(json["inngangsvilkårVurdertId"].asText())
            )
        }

    }
}
