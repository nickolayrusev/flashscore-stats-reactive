package com.jboxers.flashscore.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

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

    @GetMapping("/games/{date}")
    Mono<ByteBuffer> games(@PathVariable("date") String date) {
        return this.connection.stringCommands().get(toByteBuffer(date));
    }

    private static ByteBuffer toByteBuffer(String data){
        return ByteBuffer.wrap(data.getBytes());
    }

    private static String toString(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
