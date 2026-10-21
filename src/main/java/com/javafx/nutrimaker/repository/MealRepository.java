package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.*;
import com.google.gson.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.*;

import static com.javafx.nutrimaker.database.DatabaseClient.*;

/**
 * Repositorio adaptado a COMIDA e INGREDIENTE_COMIDA.
 * Como no esquema actual no existe MEALBASE, las filas COMIDA existentes se usan como recetas fuente
 * y se copian a la dieta nueva, junto con sus ingredientes.
 */
public class MealRepository {
    private final DatabaseClient db = new DatabaseClient();

    private static final String MEAL_SELECT = """
        SELECT ID_COMIDA AS meal_base_id, NOMBRE_COMIDA AS name,
               TIPO_COMIDA AS meal_type, GRUPO_COMIDA AS meal_group,
               CALORIAS AS calories, GRASAS AS fat, COLESTEROL AS cholesterol,
               SODIO AS sodium, CARBOHIDRATOS AS carbohydrates, PROTEINA AS protein,
               CALCIO AS calcium, HIERRO AS iron
        FROM COMIDA
        """;

    public Meal getMealById(int id) throws IOException {
        return db.read(c -> loadMeal(c, id));
    }

    private static Meal loadMeal(Connection c, int id) throws SQLException, IOException {
        JsonArray rows = query(c, MEAL_SELECT + " WHERE ID_COMIDA=?", id);
        if (rows.isEmpty()) throw new IOException("No existe la comida " + id);
        Meal meal = MealAdapter.adaptAndDeserialize(rows.get(0).toString());
        JsonArray ingredients = query(c, """
            SELECT i.ID_INGREDIENTE AS ingredient_id, i.NOMBRE AS ingredient_name,
                   ic.CANTIDAD AS ingredient_amount
            FROM INGREDIENTE_COMIDA ic
            JOIN INGREDIENTE i ON i.ID_INGREDIENTE=ic.ID_INGREDIENTE
            WHERE ic.ID_COMIDA=? ORDER BY i.NOMBRE
            """, id);
        List<Ingredient> list = new ArrayList<>();
        for (JsonElement element : ingredients) list.add(new Gson().fromJson(element, Ingredient.class));
        meal.setIngredients(list);
        return meal;
    }

    private static List<String> typeAliases(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BREAKFAST", "DESAYUNO" -> List.of("BREAKFAST", "DESAYUNO");
            case "LUNCH", "COMIDA", "ALMUERZO" -> List.of("LUNCH", "COMIDA", "ALMUERZO");
            case "DINNER", "CENA" -> List.of("DINNER", "CENA");
            case "SNACK", "COLACION", "COLACIÓN", "MERIENDA" -> List.of("SNACK", "COLACION", "COLACIÓN", "MERIENDA");
            default -> List.of(normalized);
        };
    }

    /** Selecciona aleatoriamente una receta dentro del presupuesto y las exclusiones. */
    private Meal selectMeal(Connection c, double calorieBudget, String type, Set<Integer> usedMeals)
            throws SQLException, IOException {
        List<String> aliases = typeAliases(type);
        StringBuilder sql = new StringBuilder(MEAL_SELECT)
                .append(" WHERE DIA_SEMANA IS NULL AND CALORIAS > 0 AND CALORIAS <= ? AND UPPER(TRIM(TIPO_COMIDA)) IN (");
        List<Object> params = new ArrayList<>();
        params.add(calorieBudget);
        for (int i = 0; i < aliases.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            params.add(aliases.get(i));
        }
        sql.append(") ");
        if (!usedMeals.isEmpty()) {
            sql.append("AND ID_COMIDA NOT IN (");
            for (int i = 0; i < usedMeals.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(") ");
            params.addAll(usedMeals);
        }
        // Aleatoriza entre las recetas que cumplen el presupuesto; ordenar primero por calorías
        // hacía que se eligiera casi siempre la misma receta en todos los días.
        sql.append("ORDER BY RAND() LIMIT 1");
        JsonArray rows = query(c, sql.toString(), params.toArray());
        if (rows.isEmpty()) return null;
        Meal meal = MealAdapter.adaptAndDeserialize(rows.get(0).toString());
        // La generación solo necesita valores nutricionales e ID fuente; los ingredientes se copian en SQL.
        return meal;
    }

    public Meal getMealsByTypeAndCalories(int calories, String type) throws IOException {
        if (calories <= 0) return null;
        return db.read(c -> selectMeal(c, calories, type, Collections.emptySet()));
    }

    private Map<String, Object> dietData(int userId, int patientId, ValoresNutricionales values,
                                         String restDay, int mealsPerDay, String note) {
        String safeRestDay = restDay == null ? "" : restDay.toUpperCase(Locale.ROOT);
        return new Gson().fromJson(
                new Gson().toJson(new DietRequest(userId, patientId, values, safeRestDay, mealsPerDay, note)),
                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
    }

    public boolean sendDiet(int userId, int patientId, ValoresNutricionales values,
                            String restDay, int mealsPerDay, String note) throws IOException {
        return db.transaction(c -> DietRepository.insertDiet(c,
                dietData(userId, patientId, values, restDay, mealsPerDay, note)) > 0);
    }

    /**
     * Genera un plan semanal (lunes a domingo). DIA_SEMANA guarda cada ocurrencia
     * en COMIDA; las recetas pueden variar entre días, pero no se repiten dentro del mismo día.
     */
    private static int restDayNumber(String restDay) {
        if (restDay == null || restDay.isBlank()) return -1;

        String normalized = Normalizer.normalize(
                restDay.trim().toUpperCase(Locale.ROOT),
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return switch (normalized) {
            case "MONDAY", "LUNES" -> 1;
            case "TUESDAY", "MARTES" -> 2;
            case "WEDNESDAY", "MIERCOLES" -> 3;
            case "THURSDAY", "JUEVES" -> 4;
            case "FRIDAY", "VIERNES" -> 5;
            case "SATURDAY", "SABADO" -> 6;
            case "SUNDAY", "DOMINGO" -> 7;
            default -> -1;
        };
    }

    private static final class MenuCandidate {
        final List<Meal> meals;
        final Set<Integer> ids;
        final double total;
        final double deviation;

        MenuCandidate(List<Meal> meals, Set<Integer> ids, double total, double deviation) {
            this.meals = meals;
            this.ids = ids;
            this.total = total;
            this.deviation = deviation;
        }
    }

    /** Recupera recetas fuente cercanas a las calorias asignadas, sin limitarse a las inferiores. */
    private List<Meal> candidatesForSlot(Connection c, ComidaProgramada slot,
                                          Set<Integer> usedAcrossWeek) throws SQLException {
        List<String> aliases = typeAliases(slot.getTipo());
        StringBuilder sql = new StringBuilder(MEAL_SELECT)
                .append(" WHERE DIA_SEMANA IS NULL AND CALORIAS > 0 AND UPPER(TRIM(TIPO_COMIDA)) IN (");
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < aliases.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            args.add(aliases.get(i));
        }
        sql.append(")");
        if (!usedAcrossWeek.isEmpty()) {
            sql.append(" AND ID_COMIDA NOT IN (");
            for (int i = 0; i < usedAcrossWeek.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");
            args.addAll(usedAcrossWeek);
        }
        sql.append(" ORDER BY ABS(CALORIAS - ?) ASC LIMIT 60");
        args.add(slot.getCaloriasAsignadas());
        JsonArray rows = query(c, sql.toString(), args.toArray());
        List<Meal> candidates = new ArrayList<>();
        for (JsonElement row : rows) {
            candidates.add(MealAdapter.adaptAndDeserialize(row.toString()));
        }
        return candidates;
    }

    /** Busqueda de haz: considera el total diario y las proporciones de cada tiempo. */
    private List<Meal> selectDailyPlan(Connection c, List<ComidaProgramada> slots,
                                       double target, Set<Integer> usedAcrossWeek,
                                       double tolerance) throws SQLException, IOException {
        List<MenuCandidate> beam = new ArrayList<>();
        beam.add(new MenuCandidate(new ArrayList<>(), new HashSet<>(), 0, 0));
        double allocated = 0;
        for (ComidaProgramada slot : slots) {
            allocated += slot.getCaloriasAsignadas();
            List<Meal> candidates = candidatesForSlot(c, slot, usedAcrossWeek);
            if (candidates.isEmpty()) return null;
            List<MenuCandidate> expanded = new ArrayList<>();
            for (MenuCandidate partial : beam) {
                for (Meal meal : candidates) {
                    if (partial.ids.contains(meal.getMealBaseId())) continue;
                    List<Meal> nextMeals = new ArrayList<>(partial.meals);
                    nextMeals.add(meal);
                    Set<Integer> nextIds = new HashSet<>(partial.ids);
                    nextIds.add(meal.getMealBaseId());
                    double total = partial.total + meal.getCalories();
                    double deviation = partial.deviation
                            + Math.abs(meal.getCalories() - slot.getCaloriasAsignadas());
                    expanded.add(new MenuCandidate(nextMeals, nextIds, total, deviation));
                }
            }
            if (expanded.isEmpty()) return null;
            final double allocatedSoFar = allocated;
            expanded.sort(Comparator.comparingDouble(m ->
                    Math.abs(m.total - allocatedSoFar) + 0.18 * m.deviation));
            beam = new ArrayList<>(expanded.subList(0, Math.min(180, expanded.size())));
        }
        return beam.stream()
                .filter(m -> Math.abs(m.total - target) <= target * tolerance)
                .min(Comparator.comparingDouble(m ->
                        Math.abs(m.total - target) + 0.18 * m.deviation))
                .map(m -> m.meals)
                .orElse(null);
    }

    public boolean createNewDiet(double calories, int mealsPerDay, String restDay,
                                 int userId, int patientId, String note) {
        if (!Double.isFinite(calories) || calories <= 0 || calories > 6000 ||
                mealsPerDay < 1 || mealsPerDay > 6 || userId <= 0 || patientId <= 0) {
            return false;
        }

        try {
            return db.transaction(c -> {
                List<ComidaProgramada> plan =
                        DistribuidorDeCalorias.distribuir(calories, mealsPerDay);
                List<LocalTime> times =
                        DistribuidorDeCalorias.obtenerHoras(mealsPerDay);

                ValoresNutricionales weeklyTotals = new ValoresNutricionales();
                List<List<Meal>> mealsByDay = new ArrayList<>();
                int restDayIndex = restDayNumber(restDay);
                int activeDays = 0;
                Set<Integer> usedAcrossWeek = new HashSet<>();

                // Busca combinaciones completas, no recetas aleatorias por debajo del limite.
                // Tolerancia diaria configurable: +/-10% del objetivo solicitado.
                final double tolerance = 0.10;
                for (int day = 1; day <= 7; day++) {
                    List<Meal> selectedForDay = new ArrayList<>();
                    if (day != restDayIndex) {
                        activeDays++;
                        selectedForDay = selectDailyPlan(c, plan, calories, usedAcrossWeek, tolerance);
                        if (selectedForDay == null) {
                            throw new IOException("No se encontro un menu para el dia " + day
                                    + " dentro de +/-10% de " + Math.round(calories)
                                    + " kcal sin repetir recetas. Amplia el catalogo o ajusta el objetivo.");
                        }
                        for (Meal meal : selectedForDay) {
                            usedAcrossWeek.add(meal.getMealBaseId());
                            weeklyTotals.agregar(meal);
                        }
                    }
                    mealsByDay.add(selectedForDay);
                }

                if (activeDays == 0) {
                    throw new IOException("La dieta debe tener al menos un día activo.");
                }

                // DIETA conserva los valores nutricionales promedio por día activo.
                ValoresNutricionales dailyAverage = new ValoresNutricionales();
                dailyAverage.setCalories(weeklyTotals.getCalories() / activeDays);
                dailyAverage.setFat(weeklyTotals.getFat() / activeDays);
                dailyAverage.setCholesterol(weeklyTotals.getCholesterol() / activeDays);
                dailyAverage.setSodium(weeklyTotals.getSodium() / activeDays);
                dailyAverage.setCarbohydrates(weeklyTotals.getCarbohydrates() / activeDays);
                dailyAverage.setProtein(weeklyTotals.getProtein() / activeDays);
                dailyAverage.setCalcium(weeklyTotals.getCalcium() / activeDays);
                dailyAverage.setIron(weeklyTotals.getIron() / activeDays);

                int dietId = DietRepository.insertDiet(
                        c,
                        dietData(userId, patientId, dailyAverage, restDay, mealsPerDay, note)
                );

                for (int dayIndex = 0; dayIndex < mealsByDay.size(); dayIndex++) {
                    List<Meal> mealsForDay = mealsByDay.get(dayIndex);

                    for (int mealIndex = 0; mealIndex < mealsForDay.size(); mealIndex++) {
                        DietRepository.copyMealToDiet(
                                c,
                                mealsForDay.get(mealIndex).getMealBaseId(),
                                dietId,
                                times.get(mealIndex),
                                dayIndex + 1
                        );
                    }
                }

                return true;
            });
        } catch (IOException e) {
            System.err.println("No se pudo crear la dieta: " + e.getMessage());
            return false;
        }
    }
}
