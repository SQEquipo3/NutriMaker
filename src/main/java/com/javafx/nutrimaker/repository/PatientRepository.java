package com.javafx.nutrimaker.repository;
import com.javafx.nutrimaker.database.DatabaseClient;
import java.io.IOException;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class PatientRepository {
    private final DatabaseClient db = new DatabaseClient();
    public String getAllPatients() throws IOException {
        return db.read(c -> items(query(c, "SELECT * FROM patient ORDER BY patient_id")));
    }
    public String getPatientById(int id) throws IOException {
        return db.read(c -> one(query(c, "SELECT * FROM patient WHERE patient_id=?", id)));
    }
    public int createPatientAndGetId(String name, int age, Double weight, Double height) throws IOException {
        return db.read(c -> insert(c, "INSERT INTO patient(name,age,weight,height) VALUES (?,?,?,?)", name, age, weight, height));
    }
    public boolean createPatient(String name, int age, Double weight, Double height) {
        try { createPatientAndGetId(name, age, weight, height); return true; }
        catch (IOException e) { e.printStackTrace(); return false; }
    }
    public String updatePatient(int id, String name, int age, Double weight, Double height) throws IOException {
        return db.read(c -> {
            execute(c, "UPDATE patient SET name=?,age=?,weight=?,height=? WHERE patient_id=?", name,age,weight,height,id);
            return one(query(c, "SELECT * FROM patient WHERE patient_id=?", id));
        });
    }
    public String deletePatient(int id) throws IOException {
        return db.read(c -> "{\"deleted\":" + execute(c, "DELETE FROM patient WHERE patient_id=?", id) + "}");
    }
}
