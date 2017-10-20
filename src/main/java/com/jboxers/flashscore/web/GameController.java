package com.jboxers.flashscore.web;

import com.jboxers.flashscore.config.AppCommandLineRunner;
import com.jboxers.flashscore.service.FlashScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
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
    private final static Logger logger = LoggerFactory.getLogger(GameController.class);

    private static final String FINAL_DATE = "final";
    private static final String TEMP_DATE = "temp";
    public static final String SHADOW_DATE = "shadow";

    public static final String FINAL_DATE_KEY = FINAL_DATE + ":";
    public static final String TEMP_DATE_KEY = TEMP_DATE + ":";
    public static final String SHADOW_DATE_KEY = SHADOW_DATE + ":";

    private final AppCommandLineRunner runner;
    private final ReactiveStringCommands stringCommands;
    private final ReactiveKeyCommands keyCommands;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory, AppCommandLineRunner runner) {
        ReactiveRedisConnection connection = factory.getReactiveConnection();
        this.runner = runner;
        this.stringCommands = connection.stringCommands();
        this.keyCommands = connection.keyCommands();
    }

    @GetMapping("/games/{date}")
    public Mono<ByteBuffer> games(@PathVariable("date") String date) {
        final String currentDate = runner.getCurrentDate();
        if (!currentDate.equals(date))
            return this.stringCommands.get(toByteBuffer(FINAL_DATE_KEY + date));
        //today
        return this.keyCommands.exists(toByteBuffer(TEMP_DATE_KEY + date)).flatMap(r -> {
            if (r) {
                logger.info("exists -- " + TEMP_DATE_KEY + date);
                return this.stringCommands.get(toByteBuffer(TEMP_DATE_KEY + date));
            } else {
                logger.info("don't exists -- " + TEMP_DATE_KEY + date);
                return this.stringCommands.get(toByteBuffer(FINAL_DATE_KEY + date));
            }
        });

    }

}
