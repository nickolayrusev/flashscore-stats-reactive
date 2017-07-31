package com.jboxers.flashscore.web;

import com.jboxers.flashscore.config.AppCommandLineRunner;
import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@RestController
public class GameController {
    private static final int CACHE_MINUTES = 1;
    private static final String finalDate = "final:";
    private static final String tempDate = "temp:";
    private static final String shadowDate = "shadow:";

    private final ReactiveRedisConnection connection;
    private final FlashScoreService flashScoreService;
    private final AppCommandLineRunner runner;
    private final ReactiveStringCommands stringCommands;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory, FlashScoreService flashScoreService, AppCommandLineRunner runner) {
        this.connection = factory.getReactiveConnection();
        this.flashScoreService = flashScoreService;
        this.runner = runner;
        this.stringCommands = this.connection.stringCommands();
    }

    @GetMapping("/games/{date}")
    public Mono<ByteBuffer> games(@PathVariable("date") String date) {
        if (!runner.getCurrentDate().equals(date))
            return this.connection.stringCommands().get(toByteBuffer(finalDate + date));

        //today
        return this.connection.keyCommands().exists(toByteBuffer(tempDate + date)).flatMap(r -> {
            if (r) {
                System.out.println("exists -- " + tempDate + date);
                return this.stringCommands.get(toByteBuffer(tempDate + date));
            } else {
                System.out.println("don't exists -- " + date);
                return this.flashScoreService
                        .fetch()
                        .doOnNext(list -> System.out.println("finished " + list.size()))
                        .map(result -> toByteBuffer(runner.serializeValues(result)))
                        .flatMap(buffer -> this.stringCommands.set(toByteBuffer(tempDate + runner.getCurrentDate()), buffer)
                                .flatMap(success -> {
                                    if (success) {
                                        return this.stringCommands
                                                .setEX(toByteBuffer(shadowDate + runner.getCurrentDate()),
                                                        toByteBuffer(""),
                                                        Expiration.from(Duration.ofMinutes(CACHE_MINUTES)))
                                                .flatMap(q -> this.stringCommands.get(toByteBuffer(tempDate + runner.getCurrentDate())));
                                    } else {
                                        return Mono.just(toByteBuffer("{}"));
                                    }
                                }));
            }
        });

    }
}
