package com.jboxers.flashscore.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.service.FlashScoreService;
import com.jboxers.flashscore.util.ByteBufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
import org.springframework.data.redis.connection.ReactiveListCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;
import static com.jboxers.flashscore.web.GameController.FINAL_DATE_KEY;
import static com.jboxers.flashscore.web.GameController.SHADOW_DATE_KEY;
import static com.jboxers.flashscore.web.GameController.TEMP_DATE_KEY;
import static java.util.stream.Collectors.toList;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@Component
public class AppCommandLineRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FlashScoreService flashScoreService;

    private final ObjectMapper objectMapper;

    private final ReactiveStringCommands stringCommands;

    private final ReactiveListCommands listCommands;

    private final ReactiveKeyCommands keyCommands;

    private static long cacheInterval() {
        return randInt(20, 25);
    }

    @Autowired
    public AppCommandLineRunner(ReactiveRedisConnectionFactory factory, FlashScoreService flashScoreService, ObjectMapper objectMapper) {
        this.flashScoreService = flashScoreService;
        this.objectMapper = objectMapper;
        this.stringCommands = factory.getReactiveConnection().stringCommands();
        this.keyCommands = factory.getReactiveConnection().keyCommands();
        this.listCommands = factory.getReactiveConnection().listCommands();
    }

    @Override
    public void run(String... strings) throws Exception {
        fetchTodayAndSave()
                .delayElement(Duration.ofSeconds(10))
                .then(fetchTomorrowAndSave())
                .subscribe();
    }

    private String serializeValuesAsString(List<?> stats) {
        try {
            return this.objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public List<Stat> deserializeValues(String s){
        try {
            return new ObjectMapper().readValue(s, new TypeReference<List<Stat>>(){} );
        } catch (IOException e) {
            System.err.println(e);
            return Collections.emptyList();
        }
    }

    private byte[] serializeValues(List<?> stats) {
        return serializeValuesAsString(stats).getBytes();
    }

    public Mono<Boolean> fetchTodayAndSave() {
        return this.flashScoreService.fetchToday()
                .map(l -> {
                    List<ByteBuffer> collect =
                            l.stream()
                                    .filter(q-> !q.getLeagueId().isEmpty() && !q.getLeagueStage().isEmpty())
                                    .map(s-> "standing" + ":" + s.getLeagueId() + ":" + s.getLeagueStage())
                                    .distinct()
                                    .map(s->ByteBufferUtils.toByteBuffer(s.getBytes()))
                                    .collect(toList());
                    return Tuples.of(l, collect);
                })
                .map(l -> Tuples.of(l.getT1(), this.listCommands.rPush(toByteBuffer("chat"), l.getT2())))
                .flatMap(q -> q.getT2().then(saveTodayData(serializeValues(q.getT1()))));
    }

    public Mono<Boolean> fetchTomorrowAndSave() {
        final String currentDate = getTomorrowDate();
        final ByteBuffer finalDateKey = toByteBuffer(TEMP_DATE_KEY + currentDate);
        return this.keyCommands.exists(finalDateKey).flatMap(r -> {
            if (!r) {
                return this.flashScoreService.fetchTomorrow().flatMap(l -> saveTomorrowData(serializeValues(l)));
            } else {
                return Mono.just(false);
            }
        });
    }

    public Mono<Boolean> fetchYesterdayAndSave() {
        final String yesterdayDate = getYesterdayDate();
        final ByteBuffer finalDateKey = toByteBuffer(FINAL_DATE_KEY + yesterdayDate);
        return this.keyCommands.exists(finalDateKey).flatMap(r -> {
            if (!r) {
                return this.flashScoreService.fetchYesterday().flatMap(l -> saveYesterdayData(serializeValues(l)));
            } else {
                return Mono.just(false);
            }
        });
    }


    public String getCurrentDate() {
        return Instant.now().atZone(ZoneId.of("UTC")).format(formatter);
    }

    public String getTomorrowDate() {
        return Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("UTC")).format(formatter);
    }

    public String getYesterdayDate() {
        return Instant.now().minus(1, ChronoUnit.DAYS).atZone(ZoneId.of("UTC")).format(formatter);
    }

    private Mono<Boolean> saveYesterdayData(byte[] buffer) {
        final String currentDate = getYesterdayDate();
        logger.info("saving data ... for ... yesterday ... " + currentDate);
        final ByteBuffer finalDateKey = toByteBuffer(FINAL_DATE_KEY + currentDate);
        return this.stringCommands.set(finalDateKey, toByteBuffer(buffer));

    }

    private Mono<Boolean> saveTodayData(byte[] buffer) {
        final String currentDate = getCurrentDate();
        final String yesterdayDate = getYesterdayDate();
        logger.info("saving data ... for ... today ... " + currentDate);

        return this.keyCommands.del(toByteBuffer(TEMP_DATE_KEY + yesterdayDate))
                .then(this.stringCommands.set(toByteBuffer(TEMP_DATE_KEY + currentDate), toByteBuffer(buffer))
                        .then(this.stringCommands.setEX(toByteBuffer(SHADOW_DATE_KEY + currentDate),
                                toByteBuffer(buffer),
                                Expiration.from(Duration.ofMinutes(cacheInterval())))));
    }

    private Mono<Boolean> saveTomorrowData(byte[] buffer) {
        final String currentDate = getTomorrowDate();
        logger.info("saving data ... for ... tomorrow ... " + currentDate);
        final ByteBuffer tempDateKey = toByteBuffer(TEMP_DATE_KEY + currentDate);
        return this.stringCommands.set(tempDateKey, toByteBuffer(buffer));
    }

    public void saveStanding() {
        this.listCommands.lLen(toByteBuffer("chat"))
                .doOnNext(v -> System.out.println("length is " + v))
                .map(q -> Flux.range(0, q.intValue()))
                .flatMapMany(q -> q)
                .flatMap(r -> this.listCommands.rPop(toByteBuffer("chat")))
                .map(ByteBufferUtils::toString)
                .flatMap(key->{
                    System.out.println(" key " + key);
                    final String leagueId = key.split(":")[1];
                    final String stage = key.split(":")[2];
                    return this.keyCommands.exists(toByteBuffer(key)).flatMap(e->{
                        return !e ?
                                this.flashScoreService
                                        .fetchStanding(leagueId, stage)
                                        .doOnError(err-> logger.error("in pipeline", err))
                                        .flatMap(l->this.stringCommands.set(toByteBuffer(key), toByteBuffer(serializeValuesAsString(l))))
                                : Mono.just(false);
                    });
                })
                .subscribe(q -> {
                    logger.info(" all saved " + q);
                });

    }

    /**
     * Returns a psuedo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimim value
     * @param max Maximim value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        return rand.nextInt((max - min) + 1) + min;
    }

}
