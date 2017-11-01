package com.jboxers.flashscore;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;

public class ReactivePlayground {
    public static void main (String args[]) throws IOException {
        Flux.just("a","b","c").delayElements(Duration.ofSeconds(5)).subscribe(s-> System.out.println(s+ " " + new Date()));
        System.in.read();
    }
}
