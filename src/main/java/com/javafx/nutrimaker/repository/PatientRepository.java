package com.javafx.nutrimaker.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.Patient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class PatientRepository {
    private final DatabaseClient db = new DatabaseClient();

    private static final String SELECT_PATIENT = """
        SELECT ID_PACIENTE AS patient_id, NOMBRE AS name, ALTURA AS height,
               PESO AS weight, EDAD AS age
        FROM PACIENTE
        """;

    public List<Patient> getAllPatients() throws IOException {
        JsonArray rows = db.read(c -> query(c, SELECT_PATIENT + " ORDER BY ID_PACIENTE"));
        List<Patient> patients = new ArrayList<>();
        for (JsonElement e : rows) patients.add(toPatient(e.getAsJsonObject()));
        return patients;
    }

    public Patient getPatientById(int id) throws IOException {
        JsonArray rows = db.read(c -> query(c, SELECT_PATIENT + " WHERE ID_PACIENTE=?", id));
        if (rows.isEmpty()) throw new IOException("No se encontró el registro solicitado.");
        return toPatient(rows.get(0).getAsJsonObject());
    }

    public int createPatientAndGetId(String name, int age, Double weight, Double height) throws IOException {
        return db.transaction(c -> {
            return insert(c, "INSERT INTO PACIENTE (NOMBRE,EDAD,PESO,ALTURA) VALUES (?,?,?,?)",
                    name, age, weight, height);
        });
    }

    public boolean createPatient(String name, int age, Double weight, Double height) {
        try { createPatientAndGetId(name, age, weight, height); return true; }
        catch (IOException e) { e.printStackTrace(); return false; }
    }

    public Patient updatePatient(int id, String name, int age, Double weight, Double height) throws IOException {
        return db.read(c -> {
            int changed = execute(c,
                    "UPDATE PACIENTE SET NOMBRE=?,EDAD=?,PESO=?,ALTURA=? WHERE ID_PACIENTE=?",
                    name, age, weight, height, id);
            if (changed == 0) throw new IOException("No existe el paciente " + id);
            JsonArray rows = query(c, SELECT_PATIENT + " WHERE ID_PACIENTE=?", id);
            return toPatient(rows.get(0).getAsJsonObject());
        });
    }

    /** Devuelve cuántos pacientes se borraron. Puede estar bloqueado por claves foráneas. */
    public int deletePatient(int id) throws IOException {
        return db.transaction(c -> execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", id));
    }

    private static Patient toPatient(JsonObject r) {
        return new Patient(
                r.get("name").getAsString(),
                r.get("age").isJsonNull() ? 0 : r.get("age").getAsInt(),
                r.get("weight").isJsonNull() ? null : r.get("weight").getAsDouble(),
                r.get("height").isJsonNull() ? null : r.get("height").getAsDouble(),
                r.get("patient_id").getAsInt());
    }
}