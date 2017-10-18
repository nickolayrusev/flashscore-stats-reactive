package com.jboxers.flashscore;

import com.jboxers.flashscore.service.FlashScoreService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FlashscoreStatsReactiveApplicationTests {

	@Autowired
	FlashScoreService flashScoreService;

	@Test
	public void contextLoads() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		System.out.println(Instant.now().atZone(ZoneId.of("UTC")).format(formatter));
	}

	@Test
	public void testFlashScoreService(){
		flashScoreService.fetchToday().block().forEach(System.out::println);
	}

	@Test
	public void testFlashScoreServiceTomorrow(){
		flashScoreService.fetchTomorrow().block().forEach(System.out::println);
	}
}
