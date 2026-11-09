package com.javafx.nutrimaker.database;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseClientIntegrationTest {

    @Test
    void shouldPreserveLocalDateTimeNearMidnight() throws Exception {
        String previousUrl = System.getProperty("nutrimaker.db.url");

        String testUrl =
                "jdbc:mysql://localhost:3306/nutrimaker_test"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try {
            System.setProperty("nutrimaker.db.url", testUrl);

            try (Connection connection = new DatabaseClient().connect()) {
                assertEquals("nutrimaker_test", connection.getCatalog());

                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT DATABASE()")) {
                    assertTrue(result.next());
                    assertEquals("nutrimaker_test", result.getString(1));
                }

                DatabaseClient.execute(
                        connection,
                        "CREATE TEMPORARY TABLE timezoneRegression "
                        + "(creationDate DATETIME)"
                );

                LocalDateTime localDateTime =
                        LocalDateTime.of(2026, 11, 9, 23, 30);

                Instant expectedInstant =
                        DatabaseClient.toInstant(localDateTime);

                DatabaseClient.execute(
                        connection,
                        "INSERT INTO timezoneRegression (creationDate) VALUES (?)",
                        expectedInstant
                );

                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT DATE_FORMAT(creationDate, '%Y-%m-%d %H:%i:%s') "
                             + "FROM timezoneRegression")) {
                    assertTrue(result.next());
                    assertEquals(
                            "2026-11-09 23:30:00",
                            result.getString(1)
                    );
                }

                JsonObject recovered = DatabaseClient.query(
                        connection,
                        "SELECT creationDate FROM timezoneRegression"
                ).get(0).getAsJsonObject();

                assertEquals(
                        expectedInstant.toString(),
                        recovered.get("creationDate").getAsString()
                );
            }
        } finally {
            if (previousUrl == null) {
                System.clearProperty("nutrimaker.db.url");
            } else {
                System.setProperty("nutrimaker.db.url", previousUrl);
            }
        }
    }
}
