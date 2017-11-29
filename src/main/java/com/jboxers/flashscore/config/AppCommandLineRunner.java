package com.jboxers.flashscore.config;

import com.jboxers.flashscore.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AppCommandLineRunner  implements CommandLineRunner{
    private final AppService appService;

    @Autowired
    public AppCommandLineRunner(AppService appService) {
        this.appService = appService;
    }

    @Override
    public void run(String... strings) throws Exception {
        this.appService.fetchTodayAndSave()
                .delayElement(Duration.ofSeconds(10))
                .then(this.appService.fetchTomorrowAndSave())
                .subscribe();
    }
}
