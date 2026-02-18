package no.nav.helse.spillkar.api

import com.auth0.jwk.JwkProvider
import io.ktor.server.auth.*

class AzureApp(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    private val clientId: String,
) {
    fun konfigurerJwtAuth(config: AuthenticationConfig) {
    }
}
