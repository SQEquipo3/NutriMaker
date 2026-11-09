package com.javafx.nutrimaker.database;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseClientTest {

    @Test
    void shouldPreserveLocalDateNearMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime localDateTime =
                LocalDateTime.of(2026, 11, 9, 23, 30);

        Instant instant = localDateTime.atZone(zone).toInstant();

        assertEquals(
                localDateTime,
                DatabaseClient.toLocalDateTime(instant)
        );

        assertEquals(
                instant,
                DatabaseClient.toInstant(localDateTime)
        );
    }
}
