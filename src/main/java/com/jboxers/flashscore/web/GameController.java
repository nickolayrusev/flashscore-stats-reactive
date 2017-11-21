package com.jboxers.flashscore.web;

import com.jboxers.flashscore.config.AppCommandLineRunner;
import com.jboxers.flashscore.domain.Standing;
import com.jboxers.flashscore.util.ByteBufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

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

    private final ReactiveStringCommands stringCommands;
    private final ReactiveKeyCommands keyCommands;
    private final AppCommandLineRunner runner;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory, AppCommandLineRunner runner) {
        this.runner = runner;
        ReactiveRedisConnection connection = factory.getReactiveConnection();
        this.stringCommands = connection.stringCommands();
        this.keyCommands = connection.keyCommands();
    }

    @GetMapping("/games/{date}")
    public Flux<ByteBuffer> games(@PathVariable("date") String date) {
        return this.keyCommands.exists(toByteBuffer(TEMP_DATE_KEY + date))
                .flatMap(r -> r ?
                        this.stringCommands.get(toByteBuffer(TEMP_DATE_KEY + date))
                        : this.stringCommands.get(toByteBuffer(FINAL_DATE_KEY + date)))
//                .doOnNext(s->System.out.println(ByteBufferUtils.toString(s)))
                .flatMapIterable(l-> runner.deserializeValues(ByteBufferUtils.toString(l)))
                .flatMap(s->{
                    System.out.println("stat is " + s);
                    return this.stringCommands.get(toByteBuffer("standing:" + s.getLeagueId() + ":"+ s.getLeagueStage())).map(r->{
//                        s.setStanding(Collections.emptyList());
                        logger.info("r " + r);
                        return s;
                    });
                })
                .map(q->ByteBuffer.wrap("asasas".getBytes()));
    }


}
