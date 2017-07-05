package com.jboxers.flashscore.service;

import com.jboxers.flashscore.domain.Game;
import com.jboxers.flashscore.domain.Stat;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by nikolayrusev on 6/30/17.
 */
@Component
public class FlashScoreService {

    private final WebClient client;

    public FlashScoreService(){
        this.client = WebClient.create();
    }

    private static final String URL = "http://www.flashscore.mobi/";

    public Mono<List<Stat>> fetch() {
        return fetchData(URL)
                .map(this::extractUrls)
                .doOnNext(System.out::println)
                .doOnNext(s-> System.out.println("all ids are "+s.size()))
                .flatMapIterable(q->q)
                .map(s -> s.substring(7, 15))
                .map(s -> "http://d.flashscore.com/x/feed/d_hh_" + s + "_en_1")
                .flatMap(this::fetchData)
                .map(this::extractHeadToHead)
                .collectList();
    }

    private List<String> extractUrls(String data) {
        return Jsoup.parse(data).select("div[id=score-data] > a").eachAttr("href");
    }

    private Mono<String> fetchData(String uri) {
        return this.client
                .get()
                .uri(uri)
                .header("X-Fsign","SW9D1eZo")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(35))
                .log("category", Level.ALL, SignalType.ON_ERROR, SignalType.ON_COMPLETE, SignalType.CANCEL, SignalType.REQUEST);
    }


    private Stat extractHeadToHead(String data) {
        Elements select = Jsoup.parse(data).select("#tab-h2h-overall .h2h_mutual");
        String title = select.select("thead > tr").text().substring("Head-to-head matches: ".length());
        Elements tr = select.select("tbody > tr");

        List<Game> games = tr.stream().filter(q -> q.children().size() >= 5).map(q -> {
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

        return Stat.builder().games(games).id(title).build();
    }
}
