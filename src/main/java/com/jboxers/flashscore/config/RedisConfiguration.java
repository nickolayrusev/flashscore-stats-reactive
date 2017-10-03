package com.jboxers.flashscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static com.jboxers.flashscore.web.GameController.*;

@Configuration
public class RedisConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    @Bean
    public RedisTemplate redisTemplate (){
       RedisTemplate template = new RedisTemplate<>();
       template.setConnectionFactory(redisConnectionFactory());
       return template;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("127.0.0.1",6379);
    }

    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer() {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory());

        listenerContainer.addMessageListener((message, pattern) -> {
            logger.info("handling " + new String(message.getBody()) + " " + new String(message.getChannel())
                    + " " + new String(pattern));

            RedisConnection redisConnection = redisConnectionFactory().getConnection();

            final String body =new String(message.getBody());
            if(SHADOW_DATE.equals(body.split(":")[0])) {
                final String key = body.split(":")[1];

                byte[] bytes = redisConnection
                        .stringCommands()
                        .get( (TEMP_DATE_KEY  + key).getBytes());

                redisConnection.stringCommands().set((FINAL_DATE_KEY  + key).getBytes(), bytes);
                redisConnection.keyCommands().del((TEMP_DATE_KEY + key).getBytes());
            }
        }, new PatternTopic("__key*__:expired"));

        return listenerContainer;
    }
}
