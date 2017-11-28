package com.jboxers.flashscore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.Test;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nikolayrusev on 7/28/17.
 */

public class JSoupTest {
    @Test
    public void testJsoup() throws IOException {
        File file = new File("/Users/nikolayrusev/Repository/flashscore-stats-reactive/out.html");
        Jsoup.parse(file, "utf-8").select("div[id=score-data] > a")
                .stream()
                .map(e -> {
                    String h4 = e.siblingElements()
                            .select("h4")
                            .stream()
                            .filter(i -> i.elementSiblingIndex() < e.elementSiblingIndex())
                            .sorted(Comparator.comparingInt(Element::elementSiblingIndex).reversed())
                            .findFirst()
                            .map(Element::text)
                            .orElse("");
                    return Tuples.of("","");
                }).forEach(System.out::println);
    }

    @Test
    public void testStandings() throws IOException {
        File file = new File("/Users/nikolayrusev/Repository/flashscore-stats-reactive/standings.html");
        String result = Jsoup.parse(file, "utf-8")
                .select("script")
                .stream()
                .map(Node::outerHtml)
                .filter(s->s.contains("stats2Config"))
                .findFirst()
                .orElse("{}");
        JsonNode jsonNode = new ObjectMapper().readTree(result.substring(result.indexOf("{"), result.lastIndexOf("}")+1));
        System.out.println("result is " + result);
        System.out.println("tournament " + jsonNode.get("tournament") );
        System.out.println("tournament stage " + jsonNode.get("tournamentStage") );
    }

    @Test
    public void testStand() throws IOException {
        File file = new File("/Users/nikolayrusev/Repository/flashscore-stats-reactive/stand.html");
        Jsoup.parse(file, "utf-8")
                .select(".stats-table-container tbody > tr")
                .forEach(e->{
                    System.out.println("pos " + e.children().get(0).text());
                    System.out.println("team " + e.children().get(1).text());
                    System.out.println("matches played " + e.children().get(2).text());
                    System.out.println("wins " + e.children().get(3).text());
                    System.out.println("draws " + e.children().get(4).text());
                    System.out.println("losses " + e.children().get(5).text());
                    System.out.println("difference" + e.children().get(6).text());
                    System.out.println("points "+ e.children().get(7).text());
                });

    }
    private Stream<String> stream  = Stream.of("a","b","c","-","d","e","f","-","g");

    @Test
    public void consumer(){
//        stream.collect(splitBySeparator(s->s.equals("-"))).forEach(System.out::println);
    }

    @Test
    public void testRounding(){
        float f = (float)4 / 9 * 100;
        System.out.println(Math.round(f));
    }

    @Test
    public void testInstant(){
        System.out.println(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void testReverse(){
        Stream.of("One", "Two", "Three", "Four")
                .collect(Collectors.toCollection(LinkedList::new))
                .descendingIterator().forEachRemaining(System.out::println);

        Stream<String> input = Stream.of("a","b","c","d");
        Deque<String> output =
                input.collect(Collector.of(
                        ArrayDeque::new,
                        (deq, t) -> deq.addFirst(t),
                        (d1, d2) -> { d2.addAll(d1); return d2; }));

        output.forEach(System.out::println);
    }
    @Test
    public void testSerialization() throws JsonProcessingException {
        Tuple2<String, String> tuple2 = Tuples.of("hi", "world");
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance).put("hello", "world");
        System.out.println(new ObjectMapper().writeValueAsString(tuple2));
        System.out.println(new ObjectMapper().writeValueAsString(node));
    }
}
