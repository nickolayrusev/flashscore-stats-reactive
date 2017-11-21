package com.jboxers.flashscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;

import static com.jboxers.flashscore.web.GameController.SHADOW_DATE;

@Configuration
public class RedisMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private final AppCommandLineRunner runner;
    private final RedisConnectionFactory redisConnectionFactory;

    @Autowired
    public RedisMessageListener(AppCommandLineRunner runner,
                                RedisConnectionFactory redisConnectionFactory ) {
        this.runner = runner;
        this.redisConnectionFactory = redisConnectionFactory;
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

    @Bean
    public RedisMessageListenerContainer standingExpirationListener() {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);

        listenerContainer.addMessageListener((message, pattern) -> {
            String m = new String(message.getBody());
            logger.info("received " + m);

            if(m.equals("rpush"))
                this.runner.saveStanding();
        }, new PatternTopic("__keyspace@0__:chat"));

        return listenerContainer;
    }
}
