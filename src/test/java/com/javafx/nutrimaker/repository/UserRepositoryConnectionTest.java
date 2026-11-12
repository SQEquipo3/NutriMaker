package com.javafx.nutrimaker.repository;

import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRepositoryConnectionTest {

    @Test
    void propagarErroresDeConexion() {
        String previousUrl = System.getProperty("nutrimaker.db.url");

        try {
            System.setProperty("nutrimaker.db.url",
                    "jdbc:mysql://127.0.0.1:1/nutrimaker_test?connectTimeout=1000");

            UserRepository users = new UserRepository();

            assertThrows(UncheckedIOException.class,
                    () -> users.verifyPasswordByEmail("test@nutri.test", "Prueba1234"));

            assertThrows(UncheckedIOException.class,
                    () -> users.getIdByEmail("test@nutri.test"));

            assertThrows(UncheckedIOException.class,
                    () -> users.insertUser("test@nutri.test", "Prueba1234"));
        } finally {
            if (previousUrl == null) {
                System.clearProperty("nutrimaker.db.url");
            } else {
                System.setProperty("nutrimaker.db.url", previousUrl);
            }
        }
    }
}
