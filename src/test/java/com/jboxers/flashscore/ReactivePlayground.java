package com.jboxers.flashscore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReactivePlayground {
    public static void main(String args[]) throws IOException {
//        Flux.just("a","b","c").delayElements(Duration.ofSeconds(5)).subscribe(s-> System.out.println(s+ " " + new Date()));
        ReactivePlayground playground = new ReactivePlayground();
        playground.getMono().flatMapIterable(playground::splitByComma).map(playground::block)
                .subscribe(s -> {
                    System.out.println("finished " + s);
                });
        System.in.read();
    }

    String block(String data) {
        System.out.println("entering ...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "received " + data +  " "  + new Date();
    }

    List<String> splitByComma(String s) {
        return Arrays.asList(s.split(","));
    }

    Mono<String> getMono() {
        String items = "a,b,c,d,e,f,g";
        return Mono.just(items);
    }

}
