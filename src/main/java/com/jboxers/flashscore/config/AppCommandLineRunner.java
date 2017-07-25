package com.jboxers.flashscore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;

/**
 * Created by nikolayrusev on 7/12/17.
 */
@Component
public class AppCommandLineRunner implements CommandLineRunner {

    private ReactiveRedisConnection connection;

    @Autowired
    public AppCommandLineRunner(ReactiveRedisConnectionFactory factory) {
        this.connection = factory.getReactiveConnection();
    }

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("saving some data");
        Mono<Boolean> first = this.connection
                .stringCommands()
                .set(ByteBuffer.wrap("prefix-2".getBytes()), ByteBuffer.wrap("Ivan".getBytes()))
                .doOnError(Throwable::printStackTrace);

        Mono<Boolean> second = this.connection
                .stringCommands()
                .set(ByteBuffer.wrap("prefix-3".getBytes()), ByteBuffer.wrap("Petkan".getBytes()))
                .doOnError(Throwable::printStackTrace);

        Mono<Boolean> third = this.connection.stringCommands().setEX(ByteBuffer.wrap("prefix-4".getBytes()), ByteBuffer.wrap("three minutes".getBytes()), Expiration.from(Duration.ofMinutes(3)));

        first.then(second).then(third).subscribe();
    }
}
