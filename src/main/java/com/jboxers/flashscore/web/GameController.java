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
    private static final int CACHE_MINUTES = 10;
    private static final String FINAL_DATE = "final:";
    private static final String TEMP_DATE = "temp:";
    private static final String SHADOW_DATE = "shadow:";

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
        final String currentDate = runner.getCurrentDate();
        if (!currentDate.equals(date))
            return this.stringCommands.get(toByteBuffer(FINAL_DATE + date));
        //today
        return this.connection.keyCommands().exists(toByteBuffer(TEMP_DATE + date)).flatMap(r -> {
            if (r) {
                System.out.println("exists -- " + TEMP_DATE + date);
                return this.stringCommands.get(toByteBuffer(TEMP_DATE + date));
            } else {
                System.out.println("don't exists -- " + TEMP_DATE + date);
                return this.flashScoreService
                        .fetch()
                        .doOnNext(list -> System.out.println("finished " + list.size()))
                        .map(result -> toByteBuffer(runner.serializeValues(result)))
                        .flatMap(buffer -> this.stringCommands.set(toByteBuffer(TEMP_DATE + currentDate), buffer)
                                .flatMap(success -> {
                                    if (success) {
                                        return this.stringCommands
                                                .setEX(toByteBuffer(SHADOW_DATE + currentDate),
                                                        toByteBuffer(""),
                                                        Expiration.from(Duration.ofMinutes(CACHE_MINUTES)))
                                                .flatMap(q -> this.stringCommands.get(toByteBuffer(TEMP_DATE + currentDate)));
                                    } else {
                                        return Mono.just(toByteBuffer("{}"));
                                    }
                                }));
            }
        });

    }
}
