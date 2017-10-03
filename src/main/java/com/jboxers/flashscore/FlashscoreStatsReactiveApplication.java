package com.jboxers.flashscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashscoreStatsReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);
	}
}
