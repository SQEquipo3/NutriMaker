package com.javafx.nutrimaker.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.qa.TC;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.javafx.nutrimaker.database.DatabaseClient.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas ITS-06, ITS-08 e ITS-09. Únicamente operan sobre nutrimaker_test. */
public class StoredDietIntegrationTest {
    private static final String DB_NAME = "nutrimaker_test";
    private static final String TEST_URL = "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String previousUrl;

    private final DatabaseClient db = new DatabaseClient();
    private final DietRepository diets = new DietRepository();
    private final UserRepository users = new UserRepository();
    private final PatientRepository patients = new PatientRepository();
    private final List<Integer> createdDietIds = new ArrayList<>();
    private final List<Integer> createdPatientIds = new ArrayList<>();
    private final List<Integer> createdUserIds = new ArrayList<>();
    private int userA, userB, patientA, patientB;

    @BeforeAll
    static void requireTestDatabase() throws Exception {
        previousUrl = System.getProperty("nutrimaker.db.url");
        System.setProperty("nutrimaker.db.url", TEST_URL);
        try (Connection c = new DatabaseClient().connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "PROHIBIDO ejecutar contra la base principal");
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT DATABASE()")) {
                assertTrue(r.next());
                assertEquals(DB_NAME, r.getString(1), "PROHIBIDO ejecutar contra la base principal");
            }
        }
    }

    @AfterAll
    static void restoreDatabaseConfiguration() {
        if (previousUrl == null) System.clearProperty("nutrimaker.db.url");
        else System.setProperty("nutrimaker.db.url", previousUrl);
    }

    private void assertTestDatabase() throws Exception {
        try (Connection c = db.connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "Se detectó otra base de datos");
        }
    }

    @BeforeEach
    void createFixtures() throws Exception {
        assertTestDatabase();
        userA = createUser("A");
        userB = createUser("B");
        patientA = createPatient("A");
        patientB = createPatient("B");
    }

    private int createUser(String suffix) {
        String email = "qa" + suffix + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@x.mx";
        assertTrue(users.insertUser(email, "Temporal123!"), "No se pudo crear usuario de prueba");
        Integer id = users.getIdByEmail(email);
        assertNotNull(id, "No se recuperó el usuario creado");
        createdUserIds.add(id);
        return id;
    }

    private int createPatient(String suffix) throws Exception {
        int id = patients.createPatientAndGetId("QA" + UUID.randomUUID().toString().substring(0, 8), 30, 70.0, 1.70);
        createdPatientIds.add(id);
        return id;
    }

    private int createDiet(int userId, int patientId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("patient_id", patientId);
        data.put("calories", 2000);
        data.put("meals_per_day", 3);
        data.put("rest_day", "SUNDAY");
        data.put("note", "QA-ITS33-" + UUID.randomUUID());
        JsonObject row = JsonParser.parseString(diets.createDiet(data)).getAsJsonObject();
        int id = row.get("diet_id").getAsInt();
        createdDietIds.add(id);
        return id;
    }

    private int count(String sql, Object... args) throws Exception {
        return db.read(c -> query(c, sql, args).get(0).getAsJsonObject().get("n").getAsInt());
    }

    // ==========================================
    // PRUEBA ITS-06 (Listado y paginación de dietas)
    // ==========================================
    /**
     * @nombre Listar y paginar las dietas de cada usuario
     * @objetivo Verificar el listado de dietas almacenadas: DietRepository.getTotalDietsCount(usuario) y
     * DietRepository.getDiets(offset, límite, usuario) deben devolver solo las dietas de ese usuario
     * y paginarlas sin repetir. Es lo que usa DietStorageController para llenar la tabla de la
     * pantalla principal de 10 en 10 (RF-03).
     * @precondicion Base MySQL nutrimaker_test cargada desde NutrimakerDB/nutrimaker.sql; la prueba verifica
     * antes que la conexión apunta a esa base y nunca a la de desarrollo. Los registros que crea
     * se borran al terminar. Dos usuarios (A y B), cada uno con su paciente.
     * @pasos 1.- Crear 25 dietas de A y 2 de B con DietRepository.createDiet();
     * 2.- Llamar getTotalDietsCount() para A y para B;
     * 3.- Llamar getDiets(0, 10, A), getDiets(10, 10, A) y getDiets(20, 10, A);
     * 4.- Llamar getDiets(0, 10, B) y revisar en DIETA el dueño de cada dieta devuelta.
     * @esperado A tiene 25 dietas repartidas en páginas de 10, 10 y 5 sin repetirse; B tiene 2; cada usuario
     * solo recibe sus dietas.
     */
    @TC(id = "ITS-06", rf = "RF-03", modulo = "dietas-almacenadas")
    @Test
    void ITS_06_listarYPaginarDietasPorUsuario() throws Exception {
        for (int i = 0; i < 25; i++) createDiet(userA, patientA);
        for (int i = 0; i < 2; i++) createDiet(userB, patientB);

        assertEquals(25, diets.getTotalDietsCount(userA));
        assertEquals(2, diets.getTotalDietsCount(userB));
        List<DietSummary> page1 = diets.getDiets(0, 10, userA);
        List<DietSummary> page2 = diets.getDiets(10, 10, userA);
        List<DietSummary> page3 = diets.getDiets(20, 10, userA);
        assertEquals(10, page1.size());
        assertEquals(10, page2.size());
        assertEquals(5, page3.size());
        assertEquals(2, diets.getDiets(0, 10, userB).size());

        List<Integer> allIds = new ArrayList<>();
        for (DietSummary d : page1) allIds.add(d.getDietId());
        for (DietSummary d : page2) allIds.add(d.getDietId());
        for (DietSummary d : page3) allIds.add(d.getDietId());
        assertEquals(25, allIds.stream().distinct().count(), "Las páginas repiten dietas");
        for (int id : allIds) {
            assertEquals(userA, count("SELECT ID_USUARIO AS n FROM DIETA WHERE ID_DIETA=?", id));
        }
        for (DietSummary d : diets.getDiets(0, 10, userB)) {
            assertEquals(userB, count("SELECT ID_USUARIO AS n FROM DIETA WHERE ID_DIETA=?", d.getDietId()));
        }
    }

    // ==========================================
    // PRUEBA ITS-08 (Eliminación sin registros huérfanos)
    // ==========================================
    /**
     * @nombre Eliminar una dieta sin dejar registros huérfanos
     * @objetivo Verificar la eliminación de una dieta en MySQL: DietRepository.deleteDiet(id) debe borrar la
     * dieta, sus comidas (COMIDA) y los ingredientes de esas comidas (INGREDIENTE_COMIDA) sin dejar
     * registros huérfanos ni tocar otras dietas. Es lo que se ejecuta al confirmar «Eliminar» en la
     * pantalla principal (RF-07).
     * @precondicion Base MySQL nutrimaker_test cargada desde NutrimakerDB/nutrimaker.sql; la prueba verifica
     * antes que la conexión apunta a esa base y nunca a la de desarrollo. Los registros que crea
     * se borran al terminar. Dos dietas de usuarios distintos y al menos una comida del catálogo
     * con ingredientes.
     * @pasos 1.- Crear dos dietas con DietRepository.createDiet();
     * 2.- Copiar una comida con ingredientes a la primera con DietRepository.copyMealToDiet();
     * 3.- Llamar deleteDiet(primera) y leer el campo deleted;
     * 4.- Contar en DIETA, COMIDA e INGREDIENTE_COMIDA lo que queda de la primera y revisar la
     * segunda.
     * @esperado deleteDiet reporta deleted = 1; no queda la dieta, ni sus comidas, ni los ingredientes
     * copiados; la segunda dieta sigue.
     */
    @TC(id = "ITS-08", rf = "RF-07", modulo = "eliminar-dieta")
    @Test
    void ITS_08_eliminarDietaSinHuerfanos() throws Exception {
        int target = createDiet(userA, patientA);
        int untouched = createDiet(userB, patientB);
        int sourceMeal = findAvailableMeal();
        int copiedMeal = db.transaction(c -> DietRepository.copyMealToDiet(c, sourceMeal, target, null));
        int copiedIngredients = count("SELECT COUNT(*) AS n FROM INGREDIENTE_COMIDA WHERE ID_COMIDA=?", copiedMeal);

        JsonObject result = JsonParser.parseString(diets.deleteDiet(target)).getAsJsonObject();
        assertEquals(1, result.get("deleted").getAsInt());
        assertEquals(0, count("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=?", target));
        assertEquals(0, count("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", target));
        assertEquals(0, count("SELECT COUNT(*) AS n FROM INGREDIENTE_COMIDA WHERE ID_COMIDA=?", copiedMeal),
                "Quedaron relaciones de ingredientes huérfanas");
        assertTrue(copiedIngredients > 0, "La comida copiada debe tener ingredientes para verificar su eliminación");
        assertEquals(1, count("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=?", untouched));
    }

    // ==========================================
    // PRUEBA ITS-09 (Detalle, aislamiento y clonación)
    // ==========================================
    /**
     * @nombre Consultar y clonar solo dietas propias
     * @objetivo Verificar que cada usuario solo consulte y clone sus propias dietas:
     * DietRepository.getDietByIdForUser(dieta, usuario) y DietRepository.cloneDietForPatient(dieta,
     * usuario, paciente, …) rechazan dietas ajenas con IOException, y el clon conserva las comidas y
     * queda a nombre del usuario y del paciente destino. Es lo que usa la acción «Clonar» (RF-03;
     * URS_28: clonar duplicando toda la información).
     * @precondicion Base MySQL nutrimaker_test cargada desde NutrimakerDB/nutrimaker.sql; la prueba verifica
     * antes que la conexión apunta a esa base y nunca a la de desarrollo. Los registros que crea
     * se borran al terminar. Dieta del usuario A con una comida copiada del catálogo, un paciente
     * destino y un usuario B.
     * @pasos 1.- Llamar getDietByIdForUser(dieta, A) y getDietByIdForUser(dieta, B);
     * 2.- Llamar cloneDietForPatient(dieta, B, pacienteB, …);
     * 3.- Llamar cloneDietForPatient(dieta, A, pacienteDestino, …) y leer el clon;
     * 4.- Comparar el número de comidas del original y del clon, y consultar el clon como B.
     * @esperado A consulta su dieta; B no puede consultarla ni clonarla (IOException); el clon pertenece a A y
     * al paciente destino, tiene las mismas comidas que el original y B tampoco puede consultarlo.
     */
    @TC(id = "ITS-09", rf = "RF-03", modulo = "dietas-almacenadas")
    @Test
    void ITS_09_consultarYClonarSoloDietasPropias() throws Exception {
        int source = createDiet(userA, patientA);
        int targetPatient = createPatient("A-destino");
        int sourceMeal = findAvailableMeal();
        db.transaction(c -> DietRepository.copyMealToDiet(c, sourceMeal, source, null));

        JsonObject visible = JsonParser.parseString(diets.getDietByIdForUser(source, userA)).getAsJsonObject();
        assertEquals(userA, visible.get("user_id").getAsInt());
        assertThrows(IOException.class, () -> diets.getDietByIdForUser(source, userB),
                "Un usuario no debe consultar dietas ajenas");
        assertThrows(IOException.class,
                () -> diets.cloneDietForPatient(source, userB, patientB, null, null, null, null),
                "Un usuario no debe clonar dietas ajenas");

        int clone = diets.cloneDietForPatient(source, userA, targetPatient, null, null, null, null);
        createdDietIds.add(clone);
        JsonObject cloned = JsonParser.parseString(diets.getDietByIdForUser(clone, userA)).getAsJsonObject();
        assertEquals(userA, cloned.get("user_id").getAsInt());
        assertEquals(targetPatient, cloned.get("patient_id").getAsInt());
        assertEquals(count("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", source),
                     count("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", clone));
        assertThrows(IOException.class, () -> diets.getDietByIdForUser(clone, userB));
    }

    /** Usa una comida existente como origen sin modificar ni borrar el catálogo. */
    private int findAvailableMeal() throws Exception {
        return db.read(c -> {
            var rows = query(c, "SELECT c.ID_COMIDA AS id FROM COMIDA c JOIN INGREDIENTE_COMIDA ic ON ic.ID_COMIDA=c.ID_COMIDA GROUP BY c.ID_COMIDA ORDER BY c.ID_COMIDA LIMIT 1");
            assertFalse(rows.isEmpty(), "Se requiere al menos una comida con ingredientes en nutrimaker_test");
            return rows.get(0).getAsJsonObject().get("id").getAsInt();
        });
    }

    @AfterEach
    void removeOnlyTestData() throws Exception {
        assertTestDatabase();
        db.transaction(c -> {
            // Solo se limpian las dietas cuyo ID registró esta clase, incluso si alguna prueba falla.
            for (int id : createdDietIds) {
                execute(c, "DELETE FROM INGREDIENTE_COMIDA WHERE ID_COMIDA IN (SELECT ID_COMIDA FROM COMIDA WHERE ID_DIETA=?)", id);
                execute(c, "DELETE FROM COMIDA WHERE ID_DIETA=?", id);
                execute(c, "DELETE FROM DIETA WHERE ID_DIETA=?", id);
            }
            for (int id : createdPatientIds) execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", id);
            for (int id : createdUserIds) execute(c, "DELETE FROM USUARIO WHERE ID_USUARIO=?", id);
            return null;
        });
    }
}
