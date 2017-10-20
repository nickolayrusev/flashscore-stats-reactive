package com.jboxers.flashscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;

import static com.jboxers.flashscore.web.GameController.SHADOW_DATE;

@Configuration
public class RedisMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private final AppCommandLineRunner runner;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ReactiveStringCommands stringCommands;
    private final ReactiveKeyCommands keyCommands;

    @Autowired
    public RedisMessageListener(AppCommandLineRunner runner,
                                ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
                                RedisConnectionFactory redisConnectionFactory) {
        this.runner = runner;
        this.redisConnectionFactory = redisConnectionFactory;
        this.stringCommands = reactiveRedisConnectionFactory.getReactiveConnection().stringCommands();
        this.keyCommands = reactiveRedisConnectionFactory.getReactiveConnection().keyCommands();
    }

    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer() {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);

        listenerContainer.addMessageListener((message, pattern) -> {
            logger.info("handling " + new String(message.getBody()) + " " + new String(message.getChannel())
                    + " " + new String(pattern));

            final String body = new String(message.getBody());
            if (SHADOW_DATE.equals(body.split(":")[0])) {
                this.runner.fetchTodayAndSave()
                        .delayElement(Duration.ofSeconds(10))
                        .then(this.runner.fetchTomorrowAndSave())
                        .delayElement(Duration.ofSeconds(10))
                        .then(this.runner.fetchYesterdayAndSave())
                        .subscribe(s -> logger.info("all saved ... " + s));
            }
        }, new PatternTopic("__key*__:expired"));

        return listenerContainer;
    }
}
