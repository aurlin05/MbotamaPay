package com.mbotamapay.backend.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component("database")
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1")) {

            if (resultSet.next() && resultSet.getInt(1) == 1) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Available")
                        .withDetail("validationQuery", "SELECT 1")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "Unexpected query result")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
