package com.jboxers.flashscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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
}