package com.jboxers.flashscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static com.jboxers.flashscore.web.GameController.*;

@Configuration
public class RedisMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    private final AppCommandLineRunner runner;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String,String> redisTemplate;

    @Autowired
    public RedisMessageListener(AppCommandLineRunner runner,
                                RedisConnectionFactory redisConnectionFactory,
                                RedisTemplate<String, String> redisTemplate) {
        this.runner = runner;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisTemplate = redisTemplate;
    }

    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer() {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);

        listenerContainer.addMessageListener((message, pattern) -> {
            logger.info("handling " + new String(message.getBody()) + " " + new String(message.getChannel())
                    + " " + new String(pattern));


            final String body =new String(message.getBody());
            if(SHADOW_DATE.equals(body.split(":")[0])) {
                final String key = body.split(":")[1];

                String s = this.redisTemplate.opsForValue().get(TEMP_DATE_KEY + key);
                this.redisTemplate.opsForValue().set(FINAL_DATE_KEY + key , s);
                this.redisTemplate.delete(TEMP_DATE_KEY+key);

                runner.fetchAndSave().subscribe(l-> logger.info(" all saved ..." + l));
            }
        }, new PatternTopic("__key*__:expired"));

        return listenerContainer;
    }
}
