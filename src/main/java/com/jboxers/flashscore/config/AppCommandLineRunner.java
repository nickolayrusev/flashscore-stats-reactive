package com.jboxers.flashscore.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jboxers.flashscore.domain.Stat;
import com.jboxers.flashscore.service.FlashScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.jboxers.flashscore.util.ByteBufferUtils.toByteBuffer;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@Component
public class AppCommandLineRunner implements CommandLineRunner {

    private final ReactiveRedisConnection connection;

    private final FlashScoreService flashScoreService;

    private final ObjectMapper objectMapper;

    @Autowired
    public AppCommandLineRunner(ReactiveRedisConnectionFactory factory, FlashScoreService flashScoreService, ObjectMapper objectMapper) {
        this.connection = factory.getReactiveConnection();
        this.flashScoreService = flashScoreService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... strings) throws Exception {
        this.flashScoreService.fetch().subscribe(l -> {
            this.connection.stringCommands().set(toByteBuffer("temp:"+getCurrentDate()),
                    toByteBuffer(serializeValues(l))).subscribe();
        });
    }

    public String serializeValuesAsString(List<Stat> stats) {
        try {
            return this.objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }


    public byte[] serializeValues(List<Stat> stats) {
        return serializeValuesAsString(stats).getBytes();
    }

    public String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return Instant.now().atZone(ZoneId.of("UTC")).format(formatter);
    }

}
