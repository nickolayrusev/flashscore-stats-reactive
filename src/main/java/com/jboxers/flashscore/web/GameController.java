package com.jboxers.flashscore.web;

import com.jboxers.flashscore.config.AppCommandLineRunner;
import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
public class GameController {
    private static final int CACHE_MINUTES = 20;
    private static final String FINAL_DATE = "final";
    private static final String TEMP_DATE = "temp";
    public static final String SHADOW_DATE = "shadow";

    public static final String FINAL_DATE_KEY =  FINAL_DATE + ":";
    public static final String TEMP_DATE_KEY = TEMP_DATE + ":";
    private static final String SHADOW_DATE_KEY = SHADOW_DATE +":";

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
            return this.stringCommands.get(toByteBuffer(FINAL_DATE_KEY + date));
        //today
        return this.connection.keyCommands().exists(toByteBuffer(TEMP_DATE_KEY + date)).flatMap(r -> {
            if (r) {
                System.out.println("exists -- " + TEMP_DATE_KEY + date);
                return this.stringCommands.get(toByteBuffer(TEMP_DATE_KEY + date));
            } else {
                System.out.println("don't exists -- " + TEMP_DATE_KEY + date);
                return this.flashScoreService
                        .fetch()
                        .doOnNext(list -> System.out.println("finished " + list.size()))
                        .map(result -> toByteBuffer(runner.serializeValues(result)))
                        .flatMap(buffer -> this.stringCommands.set(toByteBuffer(TEMP_DATE_KEY + currentDate), buffer)
                                .flatMap(success -> {
                                    if (success) {
                                        return this.stringCommands
                                                .setEX(toByteBuffer(SHADOW_DATE_KEY  + currentDate),
                                                        toByteBuffer(""),
                                                        Expiration.from(Duration.ofMinutes(CACHE_MINUTES)))
                                                .flatMap(q -> this.stringCommands.get(toByteBuffer(TEMP_DATE_KEY + currentDate)));
                                    } else {
                                        return Mono.just(toByteBuffer("{}"));
                                    }
                                }));
            }
        });

    }
}
