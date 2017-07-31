package com.jboxers.flashscore.web;

import com.jboxers.flashscore.config.AppCommandLineRunner;
import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@RestController
public class GameController {
    private final ReactiveRedisConnection connection;
    private final FlashScoreService flashScoreService;
    private final AppCommandLineRunner runner;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory, FlashScoreService flashScoreService, AppCommandLineRunner runner) {
        this.connection = factory.getReactiveConnection();
        this.flashScoreService = flashScoreService;
        this.runner = runner;
    }

    @GetMapping("/games/{date}")
    public Mono<ByteBuffer> games(@PathVariable("date") String date) {
        //today
        return this.connection.keyCommands().exists(toByteBuffer(date)).flatMap(r -> {
            if (r) {
                System.out.println("exists -- " + date);
                return this.connection.stringCommands().get(toByteBuffer(date));
            } else {
                System.out.println("don't exists -- " + date);
                return this.flashScoreService
                        .fetch()
                        .doOnNext(list -> System.out.println("finished " + list.size()))
                        .map(result -> ByteBuffer.wrap(runner.serializeValues(result)))
                        .flatMap(buffer -> this.connection.stringCommands().set(toByteBuffer(runner.getCurrentDate()), buffer)
                                .flatMap(success -> success ? this.connection.stringCommands().get(toByteBuffer(runner.getCurrentDate()))
                                        : Mono.just(ByteBuffer.wrap("".getBytes()))));
            }
        });

    }
}
