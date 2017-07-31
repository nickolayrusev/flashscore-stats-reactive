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

import javax.annotation.PreDestroy;

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
			final String key = new String(message.getBody()).split(":")[1];

			byte[] bytes = redisConnection
					.stringCommands()
					.get(("temp:" + key).getBytes());

			redisConnection.stringCommands().set(("final:"+key).getBytes(),bytes);
			redisConnection.keyCommands().del(("temp:" + key).getBytes());
		}, new PatternTopic("__key*__:expired"));

		return listenerContainer;
	}

	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);
	}
}
