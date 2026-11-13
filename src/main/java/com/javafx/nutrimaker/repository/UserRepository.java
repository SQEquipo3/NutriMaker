package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.google.gson.JsonArray;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.javafx.nutrimaker.models.User;
import java.util.ArrayList;
import java.util.List;

import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());
    private final DatabaseClient db = new DatabaseClient();

    public List<User> getAllUsers() throws IOException {
        JsonArray rows = db.read(c -> query(c,
                "SELECT ID_USUARIO AS user_id, CORREO AS email FROM USUARIO ORDER BY ID_USUARIO"));
        List<User> users = new ArrayList<>();
        for (JsonElement e : rows) {
            JsonObject r = e.getAsJsonObject();
            users.add(new User(r.get("email").getAsString(), null, r.get("user_id").getAsInt()));
        }
        return users;
    }

    public boolean insertUser(String email, String plainPassword) {
        try {
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            db.transaction(c ->
                    insert(c, "INSERT INTO USUARIO (CORREO,PASSWORD) VALUES (?,?)", email, hash));
            return true;
        } catch (IOException e) {
            if (isDuplicateEmail(e)) {
                logger.log(Level.INFO, "Intento de registro con correo duplicado", e);
                throw new DuplicateEmailException("El correo ya está registrado", e);
            }
            logger.log(Level.SEVERE, "Error al registrar usuario", e);
            throw new UncheckedIOException("No fue posible registrar el usuario", e);
        }
    }

    private boolean isDuplicateEmail(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlError
                    && sqlError.getErrorCode() == 1062
                    && "23000".equals(sqlError.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public Integer getIdByEmail(String email) {
        try {
            JsonArray rows = db.read(c -> query(c,
                    "SELECT ID_USUARIO AS user_id FROM USUARIO WHERE CORREO=?", email));
            return rows.isEmpty() ? null : rows.get(0).getAsJsonObject().get("user_id").getAsInt();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al consultar usuario por correo", e);
            throw new UncheckedIOException("No fue posible consultar el usuario", e);
        }
    }

    public boolean verifyPasswordByEmail(String email, String plainPassword) {
        try {
            JsonArray rows = db.read(c -> query(c,
                    "SELECT PASSWORD AS password_hash FROM USUARIO WHERE CORREO=?", email));
            if (rows.isEmpty() || rows.get(0).getAsJsonObject().get("password_hash").isJsonNull()) {
                return false;
            }
            return BCrypt.checkpw(plainPassword,
                    rows.get(0).getAsJsonObject().get("password_hash").getAsString());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al verificar credenciales", e);
            throw new UncheckedIOException("No fue posible verificar las credenciales", e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Formato de contraseña almacenada inválido", e);
            throw new IllegalStateException("No fue posible verificar las credenciales", e);
        }
    }
}
