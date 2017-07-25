package com.jboxers.flashscore.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@RestController
public class GameController {
    private ReactiveRedisConnection connection;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory) {
        this.connection = factory.getReactiveConnection();
    }

    @GetMapping("/games")
    Mono<String> namesByLastname() {
        return this.connection.stringCommands().get(ByteBuffer.wrap("prefix-1".getBytes())).doOnNext(System.out::println).map(GameController::toString);
    }


    private static String toString(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
