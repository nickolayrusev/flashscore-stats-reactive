package com.jboxers.flashscore;

import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.annotation.PreDestroy;

@SpringBootApplication
public class FlashscoreStatsReactiveApplication {

	@Autowired
	RedisConnectionFactory factory;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory("127.0.0.1",6379);
	}


	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);


//		ApplicationContext ctx = new AnnotationConfigApplicationContext(FlashscoreStatsReactiveApplication.class);
//
//		FlashScoreService bean = ctx.getBean(FlashScoreService.class);
//		bean.fetch().subscribe(s->{
//			System.out.println("finished !!! " + s.size());
//
//		},Throwable::printStackTrace);
	}
}
