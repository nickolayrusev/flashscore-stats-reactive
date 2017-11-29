package com.jboxers.flashscore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jboxers.flashscore.domain.Game;
import com.jboxers.flashscore.domain.Standing;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.util.Gzip;
import io.vavr.control.Try;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.ipc.netty.options.ClientOptions;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public FlashScoreService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(ClientOptions.Builder::disablePool))
                .build();
    }

    //standing
    //http://d.flashscore.com/x/feed/ss_1_YqNsgljq_dbCAaU6t_table_overall?hp1=CIEe04GT&hp2=tItR6sEf&e=MV81QmDf
    // http://d.flashscore.com/x/feed/ss_1_YqNsgljq_dbCAaU6t_table_overall - it will work
    //                                     tournamentId_tournamentStage
    private static final String TODAY = "http://www.flashscore.mobi/";
    private static final String TOMORROW = "http://www.flashscore.mobi/?d=1";
    private static final String YESTERDAY = "http://www.flashscore.mobi/?d=-1";

//    https://www.flashscore.com/match/hhno9nni/#match-summary

    public Mono<List<Stat>> fetchToday() {
        return fetch(TODAY);
    }

    public Mono<List<Stat>> fetchTomorrow() {
        return fetch(TOMORROW);
    }

    public Mono<List<Stat>> fetchYesterday() {
        return fetch(YESTERDAY);
    }

    public Mono<List<Standing>> fetchStanding(String league, String stage) {
        String url = "http://d.flashscore.com/x/feed/ss_1_" + league + "_" + stage + "_table_overall";

        return fetchData(url).map(data -> {
            return Jsoup.parse(data, "utf-8")
                    .select(".stats-table-container tbody > tr")
                    .stream()
                    .map(tr -> {
                        List<String> th = Jsoup.parse(data).select("table thead > tr > th").eachAttr("data-type");
                        Elements children = tr.children();
                        String participantName = children.get(th.indexOf("participant_name")).text(),
                                goals = children.get(th.indexOf("goals")).text();
                        Integer
                                rank = Integer.valueOf(children.get(th.indexOf("rank")).text().replace(".", "")),
                                matchesPlayed = Integer.valueOf(children.get(th.indexOf("matches")).text()),
                                wins = Integer.valueOf(children.get(th.indexOf("wins")).text()),
                                losses = Integer.valueOf(children.get(th.indexOf("losses")).text()),
                                points = Integer.valueOf(children.get(th.indexOf("points")).text()),
                                draws = th.indexOf("draws") != -1 ? Integer.valueOf(children.get(th.indexOf("draws")).text()) : 0;

//                        logger.info("league " + league + " stage " + stage
//                                + " team " + children.get(1).text() + " gd " + children.get(6).text());
                        return Standing.builder()
                                .position(rank)
                                .team(participantName)
                                .matchesPlayed(matchesPlayed)
                                .wins(wins)
                                .draws(draws)
                                .losses(losses)
                                .goalDifference(goals)
                                .points(points)
                                .build();
                    })
                    .collect(toList());
        });
    }

    private Mono<List<Stat>> fetch(final String url) {
        return fetchData(url)
                .map(this::extractGameMetadata)
                .doOnNext(s -> logger.info("all ids are " + s.size()))
                .flatMapIterable(q -> q)
                .map(t -> {
                    return Tuples.of("http://d.flashscore.com/x/feed/d_hh_" + t.getT1() + "_en_1",
                            t.getT2(),
                            t.getT3(),
                            t.getT4(),
                            t.getT5(),
                            "https://www.flashscore.com/match/" + t.getT1() + "/#match-summary");
                })
                .flatMap(t ->
                        Mono.zip(fetchData(t.getT1()), fetchData(t.getT6()))
                                .map(r -> Tuples.of(r.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), r.getT2()))
                )
                .map(t -> extractGames(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6()))
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

                    Deque<String> nodes = e.parent()
                            .childNodes()
                            .stream()
                            .filter(s -> s.siblingIndex() < e.siblingIndex())
                            .collect(splitBySeparator(q -> q.outerHtml().equals("<br>")))
                            .stream().map(s -> s.stream()
                                    .filter(q -> q instanceof TextNode)
                                    .map(Node::outerHtml)
                                    .map(String::trim)
                                    .filter(c -> !c.isEmpty())
                                    .collect(Collectors.joining(" "))
                            ).collect(Collector.of(
                                    ArrayDeque::new,
                                    ArrayDeque::addFirst,
                                    (d1, d2) -> {
                                        d2.addAll(d1);
                                        return d2;
                                    }));

                    return Tuples.of(e.attr("href").substring(7, 15), //url
                            league, //league
                            e.className(), //state
                            e.text(),//score
                            nodes.getFirst());//id
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
                .flatMap(response -> response.bodyToMono(ByteArrayResource.class))
                .map(s -> Gzip.decompress(s.getByteArray()))
                .timeout(Duration.ofSeconds(45))
                .retry(3, (e) -> e instanceof TimeoutException)
                .onErrorResume(e -> {
                    logger.error("error while retrieving game meta", e);
                    return Mono.empty();
                })
                .log("category", Level.OFF, SignalType.ON_ERROR, SignalType.ON_COMPLETE, SignalType.CANCEL, SignalType.REQUEST);
    }


    private Stat extractGames(String data, String champ, String status, String gameScore, String id, String standingData) {
        List<Game> headToHeadGames = parseGames(data, ".h2h_mutual");
        List<Game> homeGames = parseGames(data, ".h2h_home");
        List<Game> awayGames = parseGames(data, ".h2h_away");
        Tuple2<String, String> tournamentIdAndStage = extractTournamentIdAndStage(standingData);

        return Stat.builder()
                .headToHeadGames(headToHeadGames)
                .homeTeamGames(homeGames)
                .awayTeamGames(awayGames)
                .id(id)
                .status(status)
                .score(gameScore)
                .league(champ)
                .leagueId(tournamentIdAndStage.getT1())
                .leagueStage(tournamentIdAndStage.getT2())
                .build();
    }

    private Tuple2<String, String> extractTournamentIdAndStage(final String data) {
        return Try.of(() -> {
            String result = Jsoup.parse(data, "utf-8")
                    .select("script")
                    .stream()
                    .map(Node::outerHtml)
                    .filter(s -> s.contains("stats2Config"))
                    .findFirst()
                    .orElse("{}");

            JsonNode jsonNode = this.objectMapper.readTree(result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1));
            return Tuples.of(
                    jsonNode.get("tournament").asText(),
                    jsonNode.get("tournamentStage").asText());
        }).getOrElse(Tuples.of("", ""));
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
