package com.jboxers.flashscore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
public class FlashscoreStatsReactiveApplication {
	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);
	}
}
