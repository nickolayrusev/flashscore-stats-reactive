package com.jboxers.flashscore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class FlashscoreStatsReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlashscoreStatsReactiveApplication.class, args);

		ApplicationContext ctx = new AnnotationConfigApplicationContext(FlashscoreStatsReactiveApplication.class);

		FlashScoreService bean = ctx.getBean(FlashScoreService.class);
		bean.fetch().subscribe(s->{
			System.out.println("finished !!! " + s.size());
			try {
				new ObjectMapper().writer().writeValue(new File("sample.txt"),s);
			} catch (IOException e) {
				e.printStackTrace();
			}
		},Throwable::printStackTrace);
	}
}
