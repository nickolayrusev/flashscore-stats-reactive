package com.jboxers.flashscore.web;

import com.jboxers.flashscore.service.AppService;
import com.jboxers.flashscore.domain.FullStat;
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
    private final AppService runner;

    @Autowired
    public GameController(ReactiveRedisConnectionFactory factory, AppService runner) {
        this.runner = runner;
        ReactiveRedisConnection connection = factory.getReactiveConnection();
        this.stringCommands = connection.stringCommands();
        this.keyCommands = connection.keyCommands();
    }

    @GetMapping("/games/{date}")
    public Flux<FullStat> games(@PathVariable("date") String date) {
        return this.keyCommands.exists(toByteBuffer(TEMP_DATE_KEY + date))
                .flatMap(r -> r ?
                        this.stringCommands.get(toByteBuffer(TEMP_DATE_KEY + date))
                        : this.stringCommands.get(toByteBuffer(FINAL_DATE_KEY + date)))
                .flatMapIterable(l-> runner.deserializeStats(ByteBufferUtils.toString(l)))
                .flatMap(s->{
                    return this.stringCommands.get(toByteBuffer("standing:" + s.getLeagueId() + ":"+ s.getLeagueStage())).map(r->{
                        return new FullStat(s,runner.deserializeStandings(ByteBufferUtils.toString(r)));
                    });
                });
    }

}
