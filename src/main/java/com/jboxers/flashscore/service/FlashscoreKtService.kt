package com.jboxers.flashscore.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jboxers.flashscore.domain.Stat
import com.jboxers.flashscore.util.Gzip
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.ipc.netty.http.client.HttpClientOptions
import java.time.Duration
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.logging.Level

@Component
class FlashscoreKtService(private val mapper: ObjectMapper) {
    val webClient: WebClient = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(Consumer<HttpClientOptions.Builder> { it.disablePool() }))
            .build()

//    private fun fetch(url: String): Mono<List<Stat>> {
//        return fetchData(url)
//    }

    private fun fetchData(uri: String): Mono<String> {
        return this.webClient
                .get()
                .uri(uri)
                .header("X-Fsign", "SW9D1eZo")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Charset", "utf-8")
                .header("Pragma", "no-cache")
                .header("Cache-control", "no-cache")
                .exchange()
                .flatMap { res ->
                    if (res.statusCode().is2xxSuccessful)
                        res.bodyToMono(ByteArrayResource::class.java).map { el -> Gzip.decompress(el.byteArray) }
                    else throw IllegalArgumentException()
                }
                .timeout(Duration.ofSeconds(45))
                .retry(2, { e -> e is TimeoutException || e is IllegalStateException })
                .onErrorResume({ _ ->
                    //logger.error("error while retrieving game meta", e)
                    Mono.empty<String>()
                })
                .log("category", Level.OFF, SignalType.ON_ERROR, SignalType.ON_COMPLETE, SignalType.CANCEL, SignalType.REQUEST)
    }
}