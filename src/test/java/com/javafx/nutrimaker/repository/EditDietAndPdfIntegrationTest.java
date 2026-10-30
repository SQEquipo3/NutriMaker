package com.javafx.nutrimaker.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.javafx.nutrimaker.PDFBuilder;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.Diet;
import com.javafx.nutrimaker.qa.TC;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.javafx.nutrimaker.database.DatabaseClient.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas ITS-07 e ITS-10. Únicamente operan sobre nutrimaker_test. */
public class EditDietAndPdfIntegrationTest {
    private static final String DB_NAME = "nutrimaker_test";
    private static final String TEST_URL = "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String previousUrl;

    private final DatabaseClient db = new DatabaseClient();
    private final DietRepository diets = new DietRepository();
    private final UserRepository users = new UserRepository();
    private final PatientRepository patients = new PatientRepository();
    private Integer userId, otherUserId, patientId;
    private String patientName;

    @BeforeAll
    static void requireTestDatabase() throws Exception {
        previousUrl = System.getProperty("nutrimaker.db.url");
        System.setProperty("nutrimaker.db.url", TEST_URL);
        try (Connection c = new DatabaseClient().connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "PROHIBIDO ejecutar contra la base principal");
        }
    }

    @AfterAll
    static void restoreDatabaseConfiguration() {
        if (previousUrl == null) System.clearProperty("nutrimaker.db.url");
        else System.setProperty("nutrimaker.db.url", previousUrl);
    }

    @BeforeEach
    void createFixtures() throws Exception {
        userId = createUser();
        otherUserId = createUser();
        patientName = "QA-Paciente-" + UUID.randomUUID().toString().substring(0, 8);
        patientId = patients.createPatientAndGetId(patientName, 34, 72.5, 1.68);
    }

    private int createUser() {
        String email = "qa-its34-" + UUID.randomUUID().toString().substring(0, 8) + "@nutri.test";
        assertTrue(users.insertUser(email, "Temporal123!"), "No se pudo crear el usuario de prueba");
        Integer id = users.getIdByEmail(email);
        assertNotNull(id, "No se recuperó el usuario creado");
        return id;
    }

    /** Genera una dieta real (3 comidas, descanso en domingo) y devuelve su ID. */
    private int generateDiet(String note) throws Exception {
        assertTrue(new MealRepository().createNewDiet(2000, 3, "SUNDAY", userId, patientId, note),
                "La generación falló: comprobar el catálogo de recetas de nutrimaker_test");
        return db.read(c -> query(c, "SELECT ID_DIETA AS id FROM DIETA WHERE ID_USUARIO=? AND COMENTARIO=?",
                userId, note).get(0).getAsJsonObject().get("id").getAsInt());
    }

    /** ID_COMIDA -> "día|nombre|calorías" de cada comida de la dieta. */
    private Map<Integer, String> mealsOf(int dietId) throws Exception {
        JsonArray rows = db.read(c -> query(c,
                "SELECT ID_COMIDA AS id, DIA_SEMANA AS day, NOMBRE_COMIDA AS name, CALORIAS AS kcal " +
                "FROM COMIDA WHERE ID_DIETA=? ORDER BY ID_COMIDA", dietId));
        Map<Integer, String> meals = new LinkedHashMap<>();
        for (JsonElement e : rows) {
            JsonObject r = e.getAsJsonObject();
            meals.put(r.get("id").getAsInt(),
                    r.get("day").getAsInt() + "|" + r.get("name").getAsString() + "|" + r.get("kcal").getAsDouble());
        }
        return meals;
    }

    // ==========================================
    // PRUEBA ITS-07 (Modificar una dieta existente)
    // ==========================================
    /**
     * @objetivo Verificar que la modificación de una dieta persista en MySQL sin alterar las comidas que no cambian.
     * @precondicion nutrimaker_test con catálogo de recetas; dieta generada de 3 comidas con descanso el domingo.
     * @pasos 1.- Cambiar comentario, calorías y día de descanso (domingo a sábado) con updateDietForUser;
     * 2.- Consultar la dieta y sus comidas en MySQL; 3.- Comparar las comidas con las originales;
     * 4.- Intentar editarla con otro usuario y cambiar la cantidad de comidas.
     * @esperado Los cambios persisten; las comidas del sábado pasan al domingo y las demás quedan idénticas;
     * otro usuario y el cambio de cantidad de comidas se rechazan sin modificar nada.
     */
    @TC(id = "ITS-07", rf = "RF-06", modulo = "modificar-dieta")
    @Test
    void ITS_07_modificarDietaPersisteSinAlterarComidas() throws Exception {
        int dietId = generateDiet("QA-ITS07-" + UUID.randomUUID());
        Map<Integer, String> before = mealsOf(dietId);
        assertEquals(18, before.size(), "La dieta generada debe tener 3 comidas en 6 días");

        Map<String, Object> changes = new HashMap<>();
        changes.put("note", "Comentario editado por QA");
        changes.put("calories", 2100.0);
        changes.put("rest_day", "SATURDAY");
        changes.put("meals_per_day", 3);
        diets.updateDietForUser(dietId, userId, changes);

        JsonObject diet = JsonParser.parseString(diets.getDietByIdForUser(dietId, userId)).getAsJsonObject();
        assertEquals("Comentario editado por QA", diet.get("note").getAsString());
        assertEquals(2100.0, diet.get("calories").getAsDouble(), 0.001);
        assertEquals("SATURDAY", diet.get("rest_day").getAsString());

        Map<Integer, String> after = mealsOf(dietId);
        assertEquals(before.keySet(), after.keySet(), "La edición no debe crear ni borrar comidas");
        for (Map.Entry<Integer, String> meal : before.entrySet()) {
            String expected = meal.getValue().startsWith("6|") ? "7" + meal.getValue().substring(1) : meal.getValue();
            assertEquals(expected, after.get(meal.getKey()),
                    "Solo las comidas del sábado deben pasar al domingo; la comida " + meal.getKey() + " cambió");
        }

        Map<String, Object> otherUserChange = Map.of("note", "Edición ajena");
        assertThrows(IOException.class, () -> diets.updateDietForUser(dietId, otherUserId, otherUserChange),
                "Un usuario no debe editar dietas ajenas");
        Map<String, Object> mealsChange = Map.of("meals_per_day", 4);
        assertThrows(IOException.class, () -> diets.updateDietForUser(dietId, userId, mealsChange),
                "Cambiar la cantidad de comidas exige regenerar la dieta");
        JsonObject unchanged = JsonParser.parseString(diets.getDietByIdForUser(dietId, userId)).getAsJsonObject();
        assertEquals("Comentario editado por QA", unchanged.get("note").getAsString());
        assertEquals(3, unchanged.get("meals_per_day").getAsInt());
        assertEquals(after, mealsOf(dietId));
    }

    // ==========================================
    // PRUEBA ITS-10 (PDF con los datos recuperados de MySQL)
    // ==========================================
    /**
     * @objetivo Verificar que PDFBuilder genere un archivo válido con los datos de la dieta recuperada desde MySQL.
     * @precondicion Dieta generada con comentario y 6 días con comidas; carpeta Documents del usuario disponible.
     * @pasos 1.- Recuperar la dieta y su paciente con getDietObjectById; 2.- Generar el PDF con PDFBuilder;
     * 3.- Revisar el archivo: tamaño, cabecera %PDF, páginas y texto.
     * @esperado La dieta trae paciente y 18 comidas; el PDF se crea sin error, pesa más de 0 bytes,
     * tiene al menos una página y contiene el nombre del paciente, el comentario y las comidas.
     */
    @TC(id = "ITS-10", rf = "RF-08", modulo = "exportacion")
    @Test
    void ITS_10_generarPdfConDatosDeMySQL() throws Exception {
        String note = "QA-ITS10 comentario del nutriologo " + UUID.randomUUID().toString().substring(0, 8);
        int dietId = generateDiet(note);

        Diet diet = diets.getDietObjectById(dietId);
        assertEquals(patientName, diet.getPatient().getName());
        assertEquals(note, diet.getNote());
        assertEquals(18, diet.getMeals().size(), "La dieta recuperada debe traer sus 18 comidas");

        // PDFBuilder escribe siempre en ~/Documents/diet.pdf: se respalda y restaura el archivo del usuario.
        Path pdf = pdfPath();
        Files.createDirectories(pdf.getParent());
        Path backup = Files.exists(pdf) ? Files.move(pdf, Files.createTempFile("diet-backup", ".pdf"),
                StandardCopyOption.REPLACE_EXISTING) : null;
        try {
            generatePdf(diet);

            assertTrue(Files.exists(pdf), "No se creó el PDF");
            assertTrue(Files.size(pdf) > 0, "El PDF está vacío");
            byte[] header = new byte[4];
            try (var in = Files.newInputStream(pdf)) { assertEquals(4, in.read(header)); }
            assertEquals("%PDF", new String(header), "El archivo no tiene cabecera PDF");

            StringBuilder text = new StringBuilder();
            try (PdfDocument doc = new PdfDocument(new PdfReader(pdf.toString()))) {
                assertTrue(doc.getNumberOfPages() >= 1, "El PDF no tiene páginas");
                for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                    text.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
                }
            }
            String content = text.toString();
            assertTrue(content.contains(patientName), "El PDF no contiene el nombre del paciente");
            assertTrue(content.contains(note), "El PDF no contiene el comentario");
            assertTrue(content.contains(diet.getMeals().get(0).getName()), "El PDF no contiene las comidas");
        } finally {
            Files.deleteIfExists(pdf);
            if (backup != null) Files.move(backup, pdf);
        }
    }

    private static Path pdfPath() throws Exception {
        Field path = PDFBuilder.class.getDeclaredField("PATH");
        path.setAccessible(true);
        return Path.of((String) path.get(null));
    }

    /** generatePdf es privado: exportPdf además abre el visor del sistema, que no existe en CI. */
    private static void generatePdf(Diet diet) throws Exception {
        Method generate = PDFBuilder.class.getDeclaredMethod("generatePdf", Diet.class);
        generate.setAccessible(true);
        try {
            generate.invoke(null, diet);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @AfterEach
    void removeOnlyTestData() throws Exception {
        try (Connection c = db.connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "Se detectó otra base de datos");
        }
        db.transaction(c -> {
            execute(c, "DELETE ic FROM INGREDIENTE_COMIDA ic JOIN COMIDA co ON co.ID_COMIDA=ic.ID_COMIDA JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", userId);
            execute(c, "DELETE co FROM COMIDA co JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", userId);
            execute(c, "DELETE FROM DIETA WHERE ID_USUARIO=?", userId);
            execute(c, "DELETE FROM PACIENTE WHERE ID_PACIENTE=?", patientId);
            execute(c, "DELETE FROM USUARIO WHERE ID_USUARIO IN (?,?)", userId, otherUserId);
            return null;
        });
    }
}
