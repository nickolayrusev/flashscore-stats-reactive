package com.jboxers.flashscore;

import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.reactive.config.CorsRegistry;

import javax.annotation.PreDestroy;

import static com.jboxers.flashscore.web.GameController.*;

@SpringBootApplication
public class FlashscoreStatsReactiveApplication {

	@Autowired
	RedisConnectionFactory factory;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory("127.0.0.1",6379);
	}

	@Bean
	public RedisMessageListenerContainer keyExpirationListenerContainer() {

		RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
		listenerContainer.setConnectionFactory(redisConnectionFactory());

		listenerContainer.addMessageListener((message, pattern) -> {
			RedisConnection redisConnection = redisConnectionFactory().getConnection();
			System.out.println("handling " + new String(message.getBody()) + " " + new String(message.getChannel())
					+ " " + new String(pattern));

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

	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);
	}
}
