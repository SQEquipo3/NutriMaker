package com.javafx.nutrimaker.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.javafx.nutrimaker.database.DatabaseClient;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.javafx.nutrimaker.database.DatabaseClient.query;

/** Clona una dieta completa en una sola transacción, sin alterar la original. */
public class DietCloneRepository {
    private final DatabaseClient db = new DatabaseClient();

    public int cloneDietForPatient(int sourceDietId, int userId, int targetPatientId) throws IOException {
        if (sourceDietId <= 0 || userId <= 0 || targetPatientId <= 0)
            throw new IllegalArgumentException("Identificadores inválidos.");
        return db.transaction(c -> {
            JsonArray sourceRows = query(c, """
                SELECT ID_PACIENTE, CALORIAS, GRASAS, COLESTEROL, SODIO, CARBOHIDRATOS,
                       PROTEINA, CALCIO, HIERRO, COMENTARIO, DIA_DESCANSO,
                       GENERO_DESTINATARIO, CANTIDAD_COMIDAS
                FROM DIETA WHERE ID_DIETA=? AND ID_USUARIO=?
                """, sourceDietId, userId);
            if (sourceRows.isEmpty())
                throw new IOException("La dieta no existe o no pertenece al usuario actual.");
            JsonObject original = sourceRows.get(0).getAsJsonObject();
            if (original.get("ID_PACIENTE").getAsInt() == targetPatientId)
                throw new IOException("Selecciona otro paciente para clonar la dieta.");
            if (query(c, "SELECT ID_PACIENTE FROM PACIENTE WHERE ID_PACIENTE=?", targetPatientId).isEmpty())
                throw new IOException("El paciente destinatario ya no existe.");

            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("user_id", userId);
            fields.put("patient_id", targetPatientId);
            for (String[] entry : new String[][] {
                {"CALORIAS", "calories"}, {"GRASAS", "fat"},
                {"COLESTEROL", "cholesterol"}, {"SODIO", "sodium"},
                {"CARBOHIDRATOS", "carbohydrates"}, {"PROTEINA", "protein"},
                {"CALCIO", "calcium"}, {"HIERRO", "iron"},
                {"COMENTARIO", "note"}, {"DIA_DESCANSO", "rest_day"},
                {"GENERO_DESTINATARIO", "target_gender"},
                {"CANTIDAD_COMIDAS", "meals_per_day"}
            }) {
                JsonElement value = original.get(entry[0]);
                Object parsed = null;
                if (value != null && !value.isJsonNull()) {
                    if (value.getAsJsonPrimitive().isNumber()) parsed = value.getAsBigDecimal();
                    else parsed = value.getAsString();
                }
                fields.put(entry[1], parsed);
            }
            int newDietId = DietRepository.insertDiet(c, fields);
            JsonArray meals = query(c, """
                SELECT ID_COMIDA FROM COMIDA
                WHERE ID_DIETA=? AND DIA_SEMANA IS NOT NULL
                ORDER BY DIA_SEMANA, HORA, ID_COMIDA
                """, sourceDietId);
            if (meals.isEmpty())
                throw new IOException("La dieta original no tiene comidas programadas para clonar.");
            for (JsonElement meal : meals) {
                DietRepository.copyMealToDiet(c,
                    meal.getAsJsonObject().get("ID_COMIDA").getAsInt(), newDietId, null);
            }
            return newDietId;
        });
    }
}
