package com.jboxers.flashscore.service;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.jboxers.flashscore.domain.Game;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.util.Gzip;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.ipc.netty.options.ClientOptions;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;


import static java.util.stream.Collectors.toList;

/**
 * Created by nikolayrusev on 6/30/17.
 */
@Component
public class FlashScoreService {

    private final WebClient client;

    private final static Logger logger = LoggerFactory.getLogger(FlashScoreService.class);

    public FlashScoreService() {
        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(ClientOptions.Builder::disablePool))
                .build();
    }

    private static final String URL = "http://www.flashscore.mobi/";

    public Mono<List<Stat>> fetch() {
        return fetchData(URL)
                .map(this::extractGameMetadata)
//                .doOnNext(System.out::println)
                .doOnNext(s -> logger.info("all ids are " + s.size()))
                .flatMapIterable(q -> q)
                .map(t -> Tuples.of("http://d.flashscore.com/x/feed/d_hh_" + t.getT1().substring(7, 15) + "_en_1",
                        t.getT2(),
                        t.getT3(),
                        t.getT4(),
                        t.getT5()))
                .flatMap(t -> fetchData(t.getT1()).delayElement(Duration.ofMillis(300)).map(s -> Tuples.of(s, t.getT2(), t.getT3(), t.getT4(), t.getT5())))
                .map(t -> extractGames(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5()))
                .collectList();
    }

    /**
     * 1. url, 2. League 3. state 4. score
     * <p>
     * out.html is data
     *
     * @param data out.html
     * @return
     */
    private List<Tuple5<String, String, String, String, String>> extractGameMetadata(String data) {
        return Jsoup.parse(data).select("div[id=score-data] > a")
                .stream()
                .map(e -> {
                    String league = e.siblingElements()
                            .select("h4")
                            .stream()
                            .filter(i -> i.elementSiblingIndex() < e.elementSiblingIndex())
                            .sorted(Comparator.comparingInt(Element::elementSiblingIndex).reversed())
                            .findFirst()
                            .map(Element::text)
                            .orElse("");

                    List<String> nodes = e.parent()
                            .childNodes()
                            .stream()
                            .filter(s -> s.siblingIndex() < e.siblingIndex())
                            .collect(splitBySeparator(q -> q.outerHtml().equals("<br>")))
                            .stream().map(s->s.stream()
                                .filter(q->q instanceof TextNode)
                                .map(Node::outerHtml)
                                .map(String::trim)
                                .filter(c -> !c.isEmpty())
                                .collect(Collectors.joining(" "))
                            ).collect(toList());

                    return Tuples.of(e.attr("href"), //url
                            league, //league
                            e.className(), //state
                            e.text(),//score
                            Lists.reverse(nodes).get(0));//id
                }).collect(toList());
    }

    private Mono<String> fetchData(String uri) {
        return this.client
                .get()
                .uri(uri)
                .header("X-Fsign", "SW9D1eZo")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Charset", "utf-8")
                .header("Pragma", "no-cache")
                .header("Cache-control", "no-cache")
                .exchange()
//                .flatMap(s-> s.body(BodyExtractors.toMono(String.class)))
                .flatMap(response-> response.bodyToMono(ByteArrayResource.class))
                .map(s->Gzip.decompress(s.getByteArray()))
//                .flatMap(response -> response.bodyToMono(byte[].class))
//                .flatMapMany(s->s.body(BodyExtractors.toDataBuffers()))
//                .map(buffer -> {
//                    byte[] result = new byte[buffer.readableByteCount()];
//                    buffer.read(result);
//                    DataBufferUtils.release(buffer);
//                    return result;
//                })
//                .reduce(Bytes::concat)
//                .map(Gzip::decompress)
                .timeout(Duration.ofSeconds(45))
                .retry(3, (e) -> e instanceof TimeoutException)
                .onErrorResume(e -> {
                    logger.error("error while retrieving game meta", e);
                    return Mono.empty();
                })
                .log("category", Level.OFF, SignalType.ON_ERROR, SignalType.ON_COMPLETE, SignalType.CANCEL, SignalType.REQUEST);
    }


    private Stat extractGames(String data, String champ, String status, String gameScore, String id) {
        List<Game> headToHeadGames = parseGames(data, ".h2h_mutual");
        List<Game> homeGames = parseGames(data, ".h2h_home");
        List<Game> awayGames = parseGames(data, ".h2h_away");
        return Stat.builder()
                .headToHeadGames(headToHeadGames)
                .homeTeamGames(homeGames)
                .awayTeamGames(awayGames)
                .id(id)
                .status(status)
                .score(gameScore)
                .league(champ)
                .build();
    }

    private List<Game> parseGames(final String data, String className) {
        Elements select = Jsoup.parse(data).select("#tab-h2h-overall " + className);
        Elements tr = select.select("tbody > tr");

        return tr.stream().filter(q -> q.children().size() >= 5).map(q -> {
            Elements td = q.children();
            String date = td.get(0).text(),
                    league = td.get(1).text(),
                    home = td.get(2).text(),
                    away = td.get(3).text(),
                    score = td.get(4).text();
            return Game.builder()
                    .date(Instant.ofEpochMilli(Long.valueOf(date) * 1000)
                            .atZone(ZoneId.from(ZoneOffset.UTC))
                            .toLocalDate())
                    .home(home)
                    .away(away)
                    .league(league)
                    .score(score)
                    .build();
        }).collect(toList());

    }

    public static Collector<Node, List<List<Node>>, List<List<Node>>> splitBySeparator(Predicate<Node> sep) {
        return Collector.of(() -> new ArrayList<List<Node>>(Arrays.asList(new ArrayList<>())),
                (l, elem) -> {
                    if (sep.test(elem)) {
                        l.add(new ArrayList<>());
                    } else l.get(l.size() - 1).add(elem);
                },
                (l1, l2) -> {
                    l1.get(l1.size() - 1).addAll(l2.remove(0));
                    l1.addAll(l2);
                    return l1;
                });
    }
}
