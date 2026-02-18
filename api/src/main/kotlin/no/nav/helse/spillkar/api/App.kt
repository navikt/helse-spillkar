package no.nav.helse.spillkar.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import java.net.URI
import no.nav.helse.spillkar.api.db.DataSourceBuilder
import no.nav.helse.spillkar.api.db.DefaultDataSourceBuilder
import org.slf4j.LoggerFactory

private val logg = LoggerFactory.getLogger(::main.javaClass)
internal val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
internal val objectmapper = jacksonObjectMapper()
    .registerModules(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        logg.error("Uncaught exception: {}", e.message, e)
        sikkerlogg.error("Uncaught exception: {}", e.message, e)
    }

    val app = naisApp(
        meterRegistry = PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            PrometheusRegistry.defaultRegistry,
            Clock.SYSTEM
        ),
        objectMapper = objectmapper,
        applicationLogger = logg,
        callLogger = LoggerFactory.getLogger("no.nav.helse.spillkar.api.CallLogging"), // TODO riktig? eller er det spillkar.spillkar-api?
        timersConfig = { call, _ ->
            this
                .tag("azp_name", call.principal<JWTPrincipal>()?.get("azp_name") ?: "n/a")
                // https://github.com/linkerd/polixy/blob/main/DESIGN.md#l5d-client-id-client-id
                // eksempel: <APP>.<NAMESPACE>.serviceaccount.identity.linkerd.cluster.local
                .tag("konsument", call.request.header("L5d-Client-Id") ?: "n/a")
        },
        mdcEntries = mapOf(
            "azp_name" to { call: ApplicationCall -> call.principal<JWTPrincipal>()?.get("azp_name") },
            "konsument" to { call: ApplicationCall -> call.request.header("L5d-Client-Id") }
        ),
    ) {
        spillkar(System.getenv())
    }
    app.start(wait = true)
}

internal fun Application.spillkar(
    env: Map<String, String>,
    dataSourceBuilder: DataSourceBuilder = DefaultDataSourceBuilder(env)
) {
    val azureApp = AzureApp(
        jwkProvider = JwkProviderBuilder(URI(env.getValue("AZURE_OPENID_CONFIG_JWKS_URI")).toURL()).build(),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
    )

    authentication { azureApp.konfigurerJwtAuth(this) }

    monitor.subscribe(ApplicationStarted) {
        dataSourceBuilder.migrate()
    }

    val dataSource = dataSourceBuilder.dataSource

    routing {
    }
}
