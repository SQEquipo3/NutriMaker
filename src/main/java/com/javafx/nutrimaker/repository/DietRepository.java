package com.javafx.nutrimaker.repository;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.*;
import com.google.gson.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

public class DietRepository {
    private final DatabaseClient db = new DatabaseClient();
    private static final Set<String> COLUMNS = Set.of("user_id","patient_id","calories","fat",
        "cholesterol","sodium","carbohydrates","protein","calcium","iron","note",
        "rest_day","target_gender","meals_per_day","creation_date");
    public List<DietSummary> getDiets(int offset, int limit, int userId) throws IOException {
        if (offset < 0 || limit < 1) throw new IllegalArgumentException("PaginaciÃ³n invÃ¡lida");
        JsonArray rows = db.read(c -> query(c, """
            SELECT d.diet_id,p.name AS patient_name,p.weight,p.height,DATE(d.creation_date) AS creation_date
            FROM diet d JOIN patient p ON p.patient_id=d.patient_id
            WHERE d.user_id=? ORDER BY d.diet_id DESC LIMIT ? OFFSET ?
            """, userId,limit,offset));
        List<DietSummary> result = new ArrayList<>();
        for (JsonElement row : rows) result.add(new Gson().fromJson(row, DietSummary.class));
        return result;
    }
    public int getTotalDietsCount(int userId) throws IOException {
        return db.read(c -> query(c,"SELECT COUNT(*) AS total FROM diet WHERE user_id=?",userId)
            .get(0).getAsJsonObject().get("total").getAsInt());
    }
    public String getDietById(int id) throws IOException {
        return db.read(c -> one(query(c,"SELECT * FROM diet WHERE diet_id=?",id)));
    }
    public String getDietsByUserId(int id) throws IOException {
        return db.read(c -> items(query(c,"SELECT * FROM diet WHERE user_id=? ORDER BY diet_id DESC",id)));
    }
    public String getDietsByPatientId(int id) throws IOException {
        return db.read(c -> items(query(c,"SELECT * FROM diet WHERE patient_id=? ORDER BY diet_id DESC",id)));
    }
    private static Map<String,Object> validated(Map<String,Object> data) {
        if (data.isEmpty() || !COLUMNS.containsAll(data.keySet()))
            throw new IllegalArgumentException("Campos de dieta invÃ¡lidos");
        Map<String,Object> values = new LinkedHashMap<>(data);
        if (values.get("creation_date") instanceof String date)
            values.put("creation_date", java.time.LocalDateTime.ofInstant(Instant.parse(date),java.time.ZoneOffset.UTC));
        return values;
    }
    static int insertDiet(Connection c, Map<String,Object> data) throws SQLException {
        Map<String,Object> values = validated(data);
        return insert(c,"INSERT INTO diet (" + String.join(",",values.keySet()) + ") VALUES ("
            + String.join(",",Collections.nCopies(values.size(),"?")) + ")",values.values().toArray());
    }
    public String createDiet(Map<String,Object> data) throws IOException {
        return db.read(c -> one(query(c,"SELECT * FROM diet WHERE diet_id=?",insertDiet(c,data))));
    }
    public String updateDiet(int id, Map<String,Object> data) throws IOException {
        Map<String,Object> values = validated(data);
        List<Object> args = new ArrayList<>(values.values());
        args.add(id);
        String assignments = String.join(",",values.keySet().stream().map(k -> k+"=?").toList());
        return db.read(c -> {
            execute(c,"UPDATE diet SET "+assignments+" WHERE diet_id=?",args.toArray());
            return one(query(c,"SELECT * FROM diet WHERE diet_id=?",id));
        });
    }
    public String deleteDiet(int id) throws IOException {
        return db.read(c -> "{\"deleted\":"+execute(c,"DELETE FROM diet WHERE diet_id=?",id)+"}");
    }
    public boolean cloneDietById(int id) throws IOException {
        return db.transaction(c -> {
            JsonArray rows = query(c,"SELECT * FROM diet WHERE diet_id=?",id);
            if (rows.isEmpty()) return false;
            Map<String,Object> data = new Gson().fromJson(rows.get(0),new com.google.gson.reflect.TypeToken<Map<String,Object>>(){}.getType());
            data.remove("diet_id");
            data.remove("creation_date");
            int newId = insertDiet(c,data);
            execute(c,"""
                INSERT INTO diet_meal(diet_id,meal_base_id,day,time_of_day,meal_type)
                SELECT ?,meal_base_id,day,time_of_day,meal_type FROM diet_meal WHERE diet_id=?
                """,newId,id);
            return true;
        });
    }
    public Diet getDietObjectById(int id) throws IOException, ParseException {
        String json = db.read(c -> {
            JsonArray rows = query(c,"""
                SELECT d.*,p.name AS patient_name,p.age,p.weight,p.height,
                dm.diet_meal_id,dm.meal_base_id,dm.day,dm.time_of_day,dm.meal_type,
                m.name AS meal_name,m.meal_group,m.calories AS meal_calories,
                m.fat AS meal_fat,m.cholesterol,m.sodium AS meal_sodium,
                m.carbohydrates,m.protein AS meal_protein,m.calcium AS meal_calcium,
                m.iron AS meal_iron,i.name AS ingredient_name,mi.amount AS ingredient_amount
                FROM diet d JOIN patient p ON p.patient_id=d.patient_id
                LEFT JOIN diet_meal dm ON dm.diet_id=d.diet_id
                LEFT JOIN mealbase m ON m.meal_base_id=dm.meal_base_id
                LEFT JOIN meal_ingredient mi ON mi.meal_base_id=m.meal_base_id
                LEFT JOIN ingredient i ON i.ingredient_id=mi.ingredient_id
                WHERE d.diet_id=? ORDER BY dm.day,dm.time_of_day,dm.diet_meal_id,i.ingredient_id
                """,id);
            if (rows.isEmpty()) throw new IOException("La dieta no existe: "+id);
            return items(rows);
        });
        return buildDietFromFlatJson(json);
    }
    public Diet buildDietFromFlatJson(String json) throws ParseException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject(); // ✅ parsea como objeto
        JsonArray rows = root.getAsJsonArray("items"); // ✅ accede al array de "items"

        Diet diet = new Diet();
        Map<Integer, Meal> mealsMap = new LinkedHashMap<>();

        for (JsonElement elem : rows) {
            JsonObject row = elem.getAsJsonObject();

            // Asignar datos generales de la dieta (sólo una vez)
            if (diet.getDietID() == 0) {
                diet.setDietID(row.get("diet_id").getAsInt());
                String fullDate = row.get("creation_date").getAsString();
                String dateOnly = fullDate.split("T")[0];

                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dateOnly);
                diet.setCreationDate(date);
                diet.setCalories(row.get("calories").getAsDouble());
                diet.setProtein(row.get("protein").getAsDouble());
                diet.setCalcium(row.get("calcium").getAsDouble());
                diet.setSodium(row.get("sodium").getAsDouble());
                diet.setIron(row.get("iron").getAsDouble());
                diet.setFats(row.get("fat").getAsDouble());
                diet.setNote(row.get("note").getAsString());

                Patient patient = new Patient(row.get("patient_name").getAsString(), row.get("age").getAsInt(), row.get("weight").getAsDouble(), row.get("height").getAsDouble(), row.get("patient_id").getAsInt());
                diet.setPatient(patient);
            }

            // Obtener meal id y verificar si ya existe
            if (row.get("meal_base_id").isJsonNull()) continue;
            int occurrenceId = row.get("diet_meal_id").getAsInt();
            int mealBaseId = row.get("meal_base_id").getAsInt();
            Meal meal = mealsMap.get(occurrenceId);
            if (meal == null) {
                meal = new Meal();
                meal.setMealBaseId(mealBaseId);
                meal.setName(row.get("meal_name").getAsString());
                meal.setIngredients(new ArrayList<>());

                // Fecha del día de la comida
                String dayString = row.get("day").getAsString(); // Ej: "2025-05-21T00:00:00Z"
                Date mealDate = java.sql.Date.valueOf(dayString.split("T")[0]);
                meal.setDay(mealDate);

                // Hora del día
                String timeString = row.get("time_of_day").getAsString(); // Ej: "2025-05-01T08:00:00Z"
                Date mealTime = Date.from(Instant.parse(timeString));
                meal.setTimeOfDay(mealTime);

                // Tipo, grupo, etc.
                meal.setMealType(row.get("meal_type").getAsString());
                meal.setMealGroup(row.get("meal_group").getAsString());

                // Nutrimentos
                meal.setCalories(row.get("meal_calories").getAsDouble());
                meal.setFat(row.get("meal_fat").getAsDouble());
                meal.setCholesterol(row.get("cholesterol").getAsDouble());
                meal.setSodium(row.get("meal_sodium").getAsDouble());
                meal.setCarbohydrates(row.get("carbohydrates").getAsDouble());
                meal.setProtein(row.get("meal_protein").getAsDouble());
                meal.setCalcium(row.get("meal_calcium").getAsDouble());
                meal.setIron(row.get("meal_iron").getAsDouble());

                mealsMap.put(occurrenceId, meal);
            }

            // Obtener ingrediente (si existe)
            if (row.has("ingredient_name") && !row.get("ingredient_name").isJsonNull()) {
                String ingredientName = row.get("ingredient_name").getAsString();

                boolean ingredientExists = meal.getIngredients().stream()
                        .anyMatch(i -> i.getName().equalsIgnoreCase(ingredientName));

                if (!ingredientExists) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setName(ingredientName);

                    if (row.has("ingredient_amount") && !row.get("ingredient_amount").isJsonNull()) {
                        ingredient.setAmount(row.get("ingredient_amount").getAsDouble());
                    }

                    // Agrega otros atributos si están disponibles
                    meal.getIngredients().add(ingredient);
                }
            }
        }

        // Finalmente setear la lista de meals en la dieta
        diet.setMeals(new ArrayList<>(mealsMap.values()));

        return diet;
    }

}



