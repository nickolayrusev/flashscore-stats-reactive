package com.jboxers.flashscore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jboxers.flashscore.domain.Game;
import com.jboxers.flashscore.domain.Standing;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.util.Gzip;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.ipc.netty.options.ClientOptions;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.nio.IntBuffer;
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


import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.toList;

/**
 * Created by nikolayrusev on 6/30/17.
 */
@Component
public class FlashScoreService {

    private final WebClient client;
    private final static Logger logger = LoggerFactory.getLogger(FlashScoreService.class);

    @Autowired
    public FlashScoreService(StringRedisTemplate stringRedisTemplate) {
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

    public Mono<List<Standing>> fetchStanding(String league, String stage){
        String url = "http://d.flashscore.com/x/feed/ss_1_" + league + "_" + stage + "_table_overall";

        return fetchData(url).map(data -> {
            return Jsoup.parse(data, "utf-8")
                    .select(".stats-table-container tbody > tr")
                    .stream()
                    .map(tr->{
                        List<String> th = Jsoup.parse(data).select("table thead > tr > th").eachAttr("data-type");
                        Elements children = tr.children();
                        String  participantName =  children.get( th.indexOf("participant_name")).text(),
                                goals = children.get(th.indexOf("goals")).text();
                        Integer
                                rank = Integer.valueOf(children.get( th.indexOf("rank") ).text().replace(".","")),
                                matchesPlayed = Integer.valueOf(children.get(th.indexOf("matches")).text()),
                                wins = Integer.valueOf(children.get(th.indexOf("wins")).text()),
                                losses = Integer.valueOf(children.get(th.indexOf("losses")).text()),
                                points = Integer.valueOf(children.get( th.indexOf("points")).text()),
                                draws = th.indexOf("draws") != -1 ? Integer.valueOf(children.get(th.indexOf("draws")).text()) :  0;

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
                            "https://www.flashscore.com/match/" + t.getT1() +"/#match-summary");
                })
                .flatMap(t -> {
                             return Mono.zip(fetchData(t.getT1()), fetchData(t.getT6())).map(r->{
                                 return Tuples.of(r.getT1(),t.getT2(), t.getT3(), t.getT4(), t.getT5(), r.getT2());
                             });
                        })
//                        fetchData(t.getT1())
//                                .delayElement(Duration.ofMillis(300))
//                                .map(s -> Tuples.of(s, t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6()))
//                                .flatMap(q->fetchData(t.getT6()).map(d-> Tuples.of(q.getT1(),q.getT2(), q.getT3(), q.getT4(), q.getT5(),d)))

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

                    List<String> nodes = e.parent()
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
                            ).collect(toList());

                    return Tuples.of(e.attr("href").substring(7, 15), //url
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

    //ugly code... tryof vavr
    private Tuple2<String,String> extractTournamentIdAndStage(final String data) {
        String result = Jsoup.parse(data, "utf-8")
                .select("script")
                .stream()
                .map(Node::outerHtml)
                .filter(s->s.contains("stats2Config"))
                .findFirst()
                .orElse("{}");
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(result.substring(result.indexOf("{"), result.lastIndexOf("}")+1));
            if(jsonNode.hasNonNull("tournament") && jsonNode.hasNonNull("tournamentStage")) {
                return Tuples.of(
                        jsonNode.get("tournament").asText(),
                        jsonNode.get("tournamentStage").asText());
            }
        } catch (IOException e) {
            return Tuples.of("","");
        }
        return Tuples.of("","");
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
