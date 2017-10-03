package com.jboxers.flashscore.config;

import com.jboxers.flashscore.service.FlashScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Calendar;
import java.util.Random;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;
import static com.jboxers.flashscore.web.GameController.CACHE_MINUTES;
import static com.jboxers.flashscore.web.GameController.SHADOW_DATE_KEY;
import static com.jboxers.flashscore.web.GameController.TEMP_DATE_KEY;

@Component
public class FlashscoreSchedulingConfigurer implements SchedulingConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(FlashscoreSchedulingConfigurer.class);

    private final FlashScoreService flashScoreService;
    private final AppCommandLineRunner runner;
//    private final RedisTemplate<String,String> redisTemplate;
    private final ReactiveStringCommands stringCommands;

    @Autowired
    public FlashscoreSchedulingConfigurer(FlashScoreService flashScoreService, AppCommandLineRunner runner, ReactiveRedisConnectionFactory factory ) {
        this.flashScoreService = flashScoreService;
        this.runner = runner;
        this.stringCommands = factory.getReactiveConnection().stringCommands();
//        this.redisTemplate = redisTemplate;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        logger.info("add task");
        scheduledTaskRegistrar.addTriggerTask(
                ()-> this.work().subscribe(),
                triggerContext -> {
                    Calendar nextExecutionTime = Calendar.getInstance();
                    nextExecutionTime.add(Calendar.MINUTE, randInt(15, 25));
                    return nextExecutionTime.getTime();
                }
        );
    }

    private Mono<byte[]> fetchData() {
        return this.flashScoreService
                .fetch()
                .doOnNext(list -> logger.info("finished " + list.size()))
                .map(runner::serializeValues);
    }

    private Mono<Boolean> saveData(byte[] buffer, String currentDate) {
//        this.redisTemplate.opsForValue().set(TEMP_DATE_KEY + currentDate, buffer);
//        this.redisTemplate.delete(SHADOW_DATE_KEY + currentDate);
//        this.redisTemplate.opsForValue().set(SHADOW_DATE_KEY + currentDate,"{}",CACHE_MINUTES,TimeUnit.MINUTES);
        logger.info("saving data ...");
        return this.stringCommands.set(toByteBuffer(TEMP_DATE_KEY + currentDate), toByteBuffer(buffer) )
                .then(this.stringCommands.setEX(toByteBuffer(SHADOW_DATE_KEY + currentDate),
                        toByteBuffer(buffer),
                        Expiration.from(Duration.ofMinutes(CACHE_MINUTES))));
    }

    private Mono<Boolean> work() {
        logger.info("Working..." + this.runner.getCurrentDate());
        return fetchData().flatMap(b-> this.saveData(b, runner.getCurrentDate()));
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
