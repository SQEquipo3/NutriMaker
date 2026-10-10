package com.javafx.nutrimaker.repository;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.Diet;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

/** Ejecutar solamente en una base de prueba con 01_schema.sql y 02_demo_data.sql. */
@EnabledIfEnvironmentVariable(named="NUTRIMAKER_RUN_DB_TESTS", matches="true")
class MySqlMigrationTest {
    private final DatabaseClient db = new DatabaseClient();
    private int count(String sql,Object... args) throws IOException {
        return db.read(c -> query(c,sql,args).get(0).getAsJsonObject().get("n").getAsInt());
    }
    @Test
    void registrationPatientsDietClonePaginationAndCascade() throws Exception {
        String email = "migration-"+UUID.randomUUID()+"+Ã±@example.test";
        UserRepository users = new UserRepository();
        PatientRepository patients = new PatientRepository();
        DietRepository diets = new DietRepository();
        int userId = 0;
        int patientId = 0;
        try {
            assertTrue(users.insertUser(email,"MigrationTest123!"));
            userId = users.getIdByEmail(email);
            assertTrue(users.verifyPasswordByEmail(email,"MigrationTest123!"));
            assertFalse(users.verifyPasswordByEmail(email,"wrong"));
            assertNull(users.getIdByEmail("' OR 1=1 -- "));
            patientId = patients.createPatientAndGetId("MarÃ­a O'Connor",32,60.5,165.0);
            patients.updatePatient(patientId,"MarÃ­a O'Connor",33,61.0,165.0);
            assertEquals(33,JsonParser.parseString(patients.getPatientById(patientId)).getAsJsonObject().get("age").getAsInt());
            assertTrue(new MealRepository().createNewDiet(1500,3,"SUNDAY",userId,patientId,"Prueba Ã±"));
            int dietId = diets.getDiets(0,1,userId).get(0).getDietId();
            assertEquals(1,diets.getTotalDietsCount(userId));
            assertEquals(0,diets.getTotalDietsCount(-1));
            assertTrue(diets.getDiets(1,1,userId).isEmpty());
            int occurrences = count("SELECT COUNT(*) n FROM diet_meal WHERE diet_id=?",dietId);
            assertTrue(occurrences>6);
            Diet diet = diets.getDietObjectById(dietId);
            assertEquals(occurrences,diet.getMeals().size(),"No deben colapsarse comidas repetidas de diferentes dÃ­as");
            assertTrue(diet.getMeals().stream().anyMatch(m -> !m.getIngredients().isEmpty()));
            assertTrue(diets.cloneDietById(dietId));
            int cloneId = diets.getDiets(0,1,userId).get(0).getDietId();
            assertNotEquals(dietId,cloneId);
            assertEquals(occurrences,count("SELECT COUNT(*) n FROM diet_meal WHERE diet_id=?",cloneId));
            diets.updateDiet(cloneId,Map.of("note","Nota actualizada"));
            assertEquals("Nota actualizada",diets.getDietObjectById(cloneId).getNote());
            assertThrows(IllegalArgumentException.class,() -> diets.updateDiet(cloneId,Map.of("bad_column","value")));
            diets.deleteDiet(cloneId);
            assertEquals(0,count("SELECT COUNT(*) n FROM diet_meal WHERE diet_id=?",cloneId));
            assertFalse(diets.cloneDietById(-1));
            assertNull(new MealRepository().getMealsByTypeAndCalories(1,"BREAKFAST"));
            assertFalse(new MealRepository().createNewDiet(1,3,null,userId,patientId,"Sin opciones"));
            assertEquals(1,diets.getTotalDietsCount(userId),"Un catÃ¡logo insuficiente no debe dejar dietas vacÃ­as");
        } finally {
            final int u = userId, p = patientId;
            db.transaction(c -> {
                execute(c,"DELETE FROM diet WHERE user_id=?",u);
                execute(c,"DELETE FROM patient WHERE patient_id=?",p);
                execute(c,"DELETE FROM useraccount WHERE email=?",email);
                return null;
            });
        }
    }
    @Test
    void failedTransactionRollsBackEarlierWrites() throws Exception {
        String marker = "rollback-"+UUID.randomUUID();
        assertThrows(IOException.class,() -> db.transaction(c -> {
            insert(c,"INSERT INTO patient(name,age,weight,height) VALUES (?,30,60,165)",marker);
            execute(c,"INSERT INTO diet_meal(diet_id,meal_base_id,day,time_of_day,meal_type) VALUES (-1,-1,CURRENT_DATE,CURRENT_TIMESTAMP,'LUNCH')");
            return null;
        }));
        assertEquals(0,count("SELECT COUNT(*) n FROM patient WHERE name=?",marker));
    }
}
