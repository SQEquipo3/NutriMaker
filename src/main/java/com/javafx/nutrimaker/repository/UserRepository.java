package com.javafx.nutrimaker.repository;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.google.gson.JsonArray;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class UserRepository {
    private final DatabaseClient db = new DatabaseClient();
    public String getAllUsers() throws IOException {
        return db.read(c -> items(query(c, "SELECT user_id, email FROM useraccount ORDER BY user_id")));
    }
    public boolean insertUser(String email, String plainPassword) {
        try {
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            db.read(c -> insert(c, "INSERT INTO useraccount(email, password) VALUES (?, ?)", email, hash));
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
    }
    public Integer getIdByEmail(String email) {
        try {
            JsonArray rows = db.read(c -> query(c, "SELECT user_id FROM useraccount WHERE email=?", email));
            return rows.isEmpty() ? null : rows.get(0).getAsJsonObject().get("user_id").getAsInt();
        } catch (IOException e) { e.printStackTrace(); return null; }
    }
    public boolean verifyPasswordByEmail(String email, String plainPassword) {
        try {
            JsonArray rows = db.read(c -> query(c, "SELECT password FROM useraccount WHERE email=?", email));
            return !rows.isEmpty() && BCrypt.checkpw(plainPassword, rows.get(0).getAsJsonObject().get("password").getAsString());
        } catch (IOException | IllegalArgumentException e) { e.printStackTrace(); return false; }
    }
}
