package com.example.tax;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    @Bean
    public ApplicationRunner resetTestDatabase(Flyway flyway) {
        return args -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
