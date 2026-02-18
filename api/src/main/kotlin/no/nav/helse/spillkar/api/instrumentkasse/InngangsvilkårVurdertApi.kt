package no.nav.helse.spillkar.api.instrumentkasse

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.inngangsvilkårVurdert() {
    post("/inngangsvilkår-vurdert") {
        håndterRequest {
            // TODO inngangsvilkårVurderthåndterer.håndter(call.json())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
