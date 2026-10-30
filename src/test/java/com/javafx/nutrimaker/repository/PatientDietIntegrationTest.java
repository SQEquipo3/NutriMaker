package com.javafx.nutrimaker.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.qa.TC;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.javafx.nutrimaker.database.DatabaseClient.*;

/** Pruebas de integración ITS-04 e ITS-05. Solo usan nutrimaker_test. */
public class PatientDietIntegrationTest {
    private static final String DB_NAME = "nutrimaker_test";
    private static final String URL = "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private final DatabaseClient db = new DatabaseClient();
    private final PatientRepository patients = new PatientRepository();
    private final UserRepository users = new UserRepository();
    private Integer patientId;
    private Integer otherPatientId;
    private Integer userId;
    private String email;

    @BeforeAll
    static void configurarBaseDePruebas() throws Exception {
        System.setProperty("nutrimaker.db.url", URL);
        // Usa las credenciales configuradas por NetBeans/Maven o variables de entorno.
        // No establece usuario root ni cambia las credenciales del equipo.
        try (Connection c = new DatabaseClient().connect()) {
            assertEquals(DB_NAME, c.getCatalog(),
                    "SEGURIDAD: se prohíbe ejecutar contra una base distinta de nutrimaker_test");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                assertTrue(rs.next());
                assertEquals(DB_NAME, rs.getString(1));
            }
        }
    }

    private void verificarBase() throws SQLException {
        try (Connection c = db.connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "No se permiten escrituras fuera de nutrimaker_test");
        }
    }

    private int crearPaciente(String nombre) throws Exception {
        verificarBase();
        return patients.createPatientAndGetId(nombre, 29, 68.5, 172.0);
    }

    private int numero(String sql, Object... args) throws Exception {
        return db.read(c -> query(c, sql, args).get(0).getAsJsonObject().get("n").getAsInt());
    }

    // ==========================================
    // PRUEBA ITS-04 (Alta de paciente en MySQL)
    // ==========================================
    /**
     * @objetivo Comprobar que RF-05 registra y recupera correctamente los datos del paciente.
     * @precondicion MySQL de XAMPP disponible y nutrimaker_test con la tabla PACIENTE.
     * @pasos 1.- Crear paciente con datos identificables; 2.- Consultarlo por su ID;
     * 3.- Comparar nombre, edad, peso y altura con los valores originales.
     * @esperado El paciente existe con ID positivo y sus datos persisten sin alteraciones.
     */
    @TC(id = "ITS-04", rf = "RF-05", modulo = "pacientes")
    @Test
    void ITS_04_registrarYConsultarPacienteEnMySQL() throws Exception {
        String nombre = "QA-ITS04-" + UUID.randomUUID().toString().substring(0, 8);
        patientId = crearPaciente(nombre);
        assertTrue(patientId > 0, "La base debe devolver un ID válido");
        JsonObject paciente = JsonParser.parseString(patients.getPatientById(patientId)).getAsJsonObject();
        assertEquals(patientId.intValue(), paciente.get("patient_id").getAsInt());
        assertEquals(nombre, paciente.get("name").getAsString());
        assertEquals(29, paciente.get("age").getAsInt());
        assertEquals(68.5, paciente.get("weight").getAsDouble(), 0.01);
        assertEquals(172.0, paciente.get("height").getAsDouble(), 0.01);
    }

    // ==========================================
    // PRUEBA ITS-05 (Generación y guardado de dieta)
    // ==========================================
    /**
     * @objetivo Verificar que RF-04 genera y guarda una dieta completa para el paciente correcto.
     * @precondicion Base nutrimaker_test con recetas fuente suficientes para seis días,
     * y permisos de INSERT/SELECT/DELETE sobre USUARIO, PACIENTE, DIETA y COMIDA.
     * @pasos 1.- Registrar usuario y dos pacientes; 2.- Generar para el primer paciente
     * una dieta de 2000 kcal, tres comidas y domingo de descanso; 3.- Consultar MySQL;
     * 4.- Verificar asociación, seis días activos, domingo vacío, calorías y variedad.
     * @esperado La dieta pertenece al paciente elegido, no al último creado;
     * tiene 18 comidas, ninguna el domingo y totales diarios dentro de +/-10%.
     */
    @TC(id = "ITS-05", rf = "RF-04", modulo = "crear-dieta")
    @Test
    void ITS_05_generarDietaParaPacienteCorrecto() throws Exception {
        verificarBase();
        email = "qa-its05-" + UUID.randomUUID().toString().substring(0, 8) + "@nutri.test";
        assertTrue(users.insertUser(email, "TestPassword123!"), "No se pudo crear el usuario temporal");
        userId = users.getIdByEmail(email);
        assertNotNull(userId, "El usuario temporal no tiene ID");

        patientId = crearPaciente("QA-Principal-" + UUID.randomUUID().toString().substring(0, 8));
        otherPatientId = crearPaciente("QA-Otro-" + UUID.randomUUID().toString().substring(0, 8));
        assertTrue(otherPatientId > patientId, "El segundo paciente debe crearse después del primero");

        String marca = "QA-ITS05-" + UUID.randomUUID();
        assertTrue(new MealRepository().createNewDiet(2000, 3, "SUNDAY", userId, patientId, marca),
                "La generación falló: comprobar que el catálogo de recetas permite 6 menús sin repetir");

        int dietaId = db.read(c -> query(c,
                "SELECT ID_DIETA AS id FROM DIETA WHERE ID_USUARIO=? AND COMENTARIO=?",
                userId, marca).get(0).getAsJsonObject().get("id").getAsInt());
        JsonObject dieta = JsonParser.parseString(new DietRepository().getDietById(dietaId)).getAsJsonObject();
        assertEquals(userId.intValue(), dieta.get("user_id").getAsInt());
        assertEquals(patientId.intValue(), dieta.get("patient_id").getAsInt(),
                "La dieta se asignó a otro paciente (riesgo #19)");
        assertNotEquals(otherPatientId.intValue(), dieta.get("patient_id").getAsInt());
        assertEquals(3, dieta.get("meals_per_day").getAsInt());
        assertEquals("SUNDAY", dieta.get("rest_day").getAsString());

        assertEquals(18, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", dietaId),
                "Deben existir tres comidas por cada uno de los seis días activos");
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=? AND DIA_SEMANA=7", dietaId),
                "El domingo debe estar libre");
        for (int dia = 1; dia <= 6; dia++) {
            assertEquals(3, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=? AND DIA_SEMANA=?", dietaId, dia),
                    "Cantidad incorrecta de comidas en el día " + dia);
        }
        assertEquals(0, numero("""
                SELECT COUNT(*) AS n FROM (
                    SELECT DIA_SEMANA, SUM(CALORIAS) AS total
                    FROM COMIDA WHERE ID_DIETA=? GROUP BY DIA_SEMANA
                    HAVING total < 1800 OR total > 2200
                ) AS fuera_de_rango
                """, dietaId), "Algún día activo quedó fuera del rango de 2000 kcal +/-10%");
        assertEquals(0, numero("""
                SELECT COUNT(*) AS n FROM COMIDA
                WHERE ID_DIETA=? AND (CALORIAS IS NULL OR CALORIAS <= 0)
                """, dietaId), "Hay comidas sin calorías válidas");
        // Presupuestos por tiempo: 30% desayuno, 40% comida, 30% cena.
        // Si falla, identifica un defecto real: el algoritmo actual optimiza
        // la suma diaria y no necesariamente limita cada comida individual.
        assertEquals(0, numero("""
                SELECT COUNT(*) AS n FROM (
                    SELECT CALORIAS,
                           ROW_NUMBER() OVER (PARTITION BY DIA_SEMANA ORDER BY HORA, ID_COMIDA) AS posicion
                    FROM COMIDA WHERE ID_DIETA=?
                ) AS tiempos
                WHERE (posicion=1 AND CALORIAS>600)
                   OR (posicion=2 AND CALORIAS>800)
                   OR (posicion=3 AND CALORIAS>600)
                """, dietaId), "Alguna comida supera su presupuesto calórico (600/800/600 kcal)");
        // El catálogo puede contener nombres repetidos, pero un mismo menú diario no debe ser idéntico.
        assertTrue(numero("SELECT COUNT(DISTINCT NOMBRE_COMIDA) AS n FROM COMIDA WHERE ID_DIETA=?", dietaId) > 3,
                "Se repitió el mismo menú durante toda la semana (riesgo #5)");
    }

    @AfterEach
    void limpiarRegistrosTemporales() throws Exception {
        // Se eliminan únicamente registros creados por esta clase; jamás datos precargados.
        verificarBase();
        db.transaction(c -> {
            if (userId != null) {
                // Primero ingredientes de las comidas generadas, después comidas y dietas.
                execute(c, "DELETE ic FROM INGREDIENTE_COMIDA ic JOIN COMIDA co ON co.ID_COMIDA=ic.ID_COMIDA JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", userId);
                execute(c, "DELETE co FROM COMIDA co JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", userId);
                execute(c, "DELETE FROM DIETA WHERE ID_USUARIO=?", userId);
            }
            if (otherPatientId != null) execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", otherPatientId);
            if (patientId != null) execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", patientId);
            if (userId != null) execute(c, "DELETE FROM USUARIO WHERE ID_USUARIO=?", userId);
            return null;
        });
    }
}
