package no.nav.helse.spillkar.spleis_modul

import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import java.net.http.HttpClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv()

    val spillkarKlient = SpillkarKlient(
        httpClient = HttpClient.newHttpClient(),
        azureTokenProvider = createAzureTokenClientFromEnvironment(env),
        env = env
    )

    val kafkaConfig = AivenConfig.default
    val consumerProducerFactory = ConsumerProducerFactory(kafkaConfig)

    RapidApplication.create(env, consumerProducerFactory = consumerProducerFactory).apply {
        InngangsvilkårVurdertRiver(this, spillkarKlient)
        InngangsvilkårsvurderingsløserRiver(this, spillkarKlient)
    }.start()
}
