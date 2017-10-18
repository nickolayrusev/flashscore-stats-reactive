package com.jboxers.flashscore.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.service.FlashScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;
import static com.jboxers.flashscore.web.GameController.SHADOW_DATE_KEY;
import static com.jboxers.flashscore.web.GameController.TEMP_DATE_KEY;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@Component
public class AppCommandLineRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private final FlashScoreService flashScoreService;

    private final ObjectMapper objectMapper;

    private final ReactiveStringCommands stringCommands;

    private static long cacheInterval(){
        return randInt(20,25);
    }

    @Autowired
    public AppCommandLineRunner(ReactiveRedisConnectionFactory factory, FlashScoreService flashScoreService, ObjectMapper objectMapper) {
        this.flashScoreService = flashScoreService;
        this.objectMapper = objectMapper;
        this.stringCommands = factory.getReactiveConnection().stringCommands();
    }

    @Override
    public void run(String... strings) throws Exception {
//        fetchTodayAndSave().delayElement(Duration.ofMillis(499)).subscribe();
    }

    public String serializeValuesAsString(List<Stat> stats) {
        try {
            return this.objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }


    public byte[] serializeValues(List<Stat> stats) {
        return serializeValuesAsString(stats).getBytes();
    }

    public Mono<Boolean> fetchTodayAndSave() {
        return this.flashScoreService.fetchToday().flatMap(l-> saveData(serializeValues(l)));
    }

    public String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return Instant.now().atZone(ZoneId.of("UTC")).format(formatter);
    }

    public String getTomorrowDate(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.of("UTC")).format(formatter);

    }

    public Mono<Boolean> saveData(byte[] buffer) {
        final String currentDate = getCurrentDate();
        logger.info("saving data ... for ... " + currentDate);
        return this.stringCommands.set(toByteBuffer(TEMP_DATE_KEY + currentDate), toByteBuffer(buffer) )
                .then(this.stringCommands.setEX(toByteBuffer(SHADOW_DATE_KEY + currentDate),
                        toByteBuffer(buffer),
                        Expiration.from(Duration.ofMinutes(cacheInterval()))));
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
