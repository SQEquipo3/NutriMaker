package com.javafx.nutrimaker;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.qa.TC;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de sistema de la issue #37. Interactúan con la interfaz JavaFX mediante
 * TestFX y utilizan exclusivamente nutrimaker_test. No alteran el esquema.
 */
public class SystemDietManagementTest extends NutriMakerGuiTest {

    private record Fixture(String email, int patientId, List<Integer> ids) {}

    /** Prepara 11 dietas para cubrir también la paginación de la pantalla. */
    private Fixture prepararOnceDietas() throws Exception {
        String email = crearUsuario();
        int patientId = crearPaciente("STS37");
        List<Integer> ids = crearDietas(email, patientId, 11);
        assertEquals(11, ids.size());
        iniciarSesion(email, PASSWORD);
        esperar("primera página de 10 dietas", () -> filas().size() == 10);
        assertTrue(visible("#nextButton"), "Con 11 dietas debe existir una página siguiente");
        return new Fixture(email, patientId, ids);
    }

    /**
     * @objetivo Verificar la edición de una dieta desde la interfaz y la persistencia de los cambios.
     * @precondicion Usuario de prueba con 11 dietas existentes.
     * @pasos Abrir edición de la primera dieta, modificar calorías y comentario, guardar y consultar MySQL.
     * @esperado Los valores nuevos persisten y el diálogo de edición se cierra.
     */
    @Test
    @TC(id = "STS-07", rf = "RF-06", modulo = "dietas")
    void STS_07_modificarDietaYPersistir() throws Exception {
        prepararOnceDietas();
        int id = filas().get(0).getDietId();
        String comentario = "QA-STS07-" + UUID.randomUUID().toString().substring(0, 8);

        pulsar(accion(0, "edit"));
        esperar("ventana Editar Dieta", () -> robot.lookup("#calTextField").tryQuery().isPresent());
        TextField calorias = robot.lookup("#calTextField").queryAs(TextField.class);
        TextField nota = robot.lookup("#commentTextField").queryAs(TextField.class);
        robot.interact(() -> { calorias.setText("2100"); nota.setText(comentario); });
        pulsar(robot.lookup("#saveButton").queryButton());
        esperar("cierre del editor", () -> robot.lookup("#calTextField").tryQuery().isEmpty());

        assertEquals(1, numero("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=? AND CALORIAS=2100 AND COMENTARIO=?",
                id, comentario), "La edición debe persistir en MySQL");
    }

    /**
     * @objetivo Validar eliminación con confirmación desde la tabla de dietas.
     * @precondicion Usuario de prueba con 11 dietas existentes.
     * @pasos Abrir la acción Eliminar, confirmar y comprobar la lista y MySQL.
     * @esperado La dieta desaparece de la tabla y de la base de datos.
     */
    @Test
    @TC(id = "STS-08", rf = "RF-07", modulo = "dietas")
    void STS_08_eliminarDietaConConfirmacion() throws Exception {
        prepararOnceDietas();
        int id = filas().get(0).getDietId();
        pulsar(accion(0, "delete"));
        esperar("diálogo de confirmación", () -> robot.lookup("#acceptBtn").tryQuery().isPresent());
        pulsar(robot.lookup("#acceptBtn").queryButton());
        esperar("eliminación persistida", () -> {
            try { return numero("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=?", id) == 0; }
            catch (Exception e) { return false; }
        });
        assertTrue(filas().stream().noneMatch(d -> d.getDietId() == id),
                "La dieta eliminada no debe seguir apareciendo en la tabla");
    }

    /**
     * @objetivo Exportar desde la interfaz una dieta completa y comprobar el contenido del PDF.
     * @precondicion Usuario con 11 dietas y una dieta generada con menú; carpeta Documents disponible.
     * @pasos Pulsar PDF, esperar el archivo y extraer el texto de sus páginas.
     * @esperado El PDF contiene nombre del paciente, comentario y al menos una comida.
     * @nota La inspección visual del formato puede registrarse adicionalmente como evidencia manual.
     */
    @Test
    @TC(id = "STS-09", rf = "RF-08", modulo = "pdf")
    void STS_09_exportarPdfConPacienteYMenu() throws Exception {
        String email = crearUsuario();
        int pacienteId = crearPaciente("PDF37");
        String comentario = "QA-STS09-" + UUID.randomUUID().toString().substring(0, 8);
        int dietaId = generarDieta(email, pacienteId, comentario);
        crearDietas(email, pacienteId, 10);
        iniciarSesion(email, PASSWORD);
        esperar("11 dietas visibles en páginas", () -> filas().size() == 10);
        assertTrue(visible("#nextButton"));

        // La ruta de PDFBuilder es fija: proteger cualquier PDF preexistente del usuario.
        Path pdf = Path.of(System.getProperty("user.home"), "Documents", "diet.pdf");
        Files.createDirectories(pdf.getParent());
        Path respaldo = null;
        if (Files.exists(pdf)) {
            respaldo = Files.createTempFile("nutrimaker-sts37-backup-", ".pdf");
            Files.move(pdf, respaldo, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            int fila = indiceDieta(dietaId);
            if (fila < 0) {
                pulsar("#nextButton");
                esperar("segunda página", () -> filas().size() == 1);
                fila = indiceDieta(dietaId);
            }
            assertTrue(fila >= 0, "La dieta completa debe aparecer en una de las dos páginas");
            pulsar(accion(fila, "pdf"));
            esperar("PDF generado", () -> {
                try { return Files.exists(pdf) && Files.size(pdf) > 100; }
                catch (Exception e) { return false; }
            });
            StringBuilder texto = new StringBuilder();
            try (PdfDocument documento = new PdfDocument(new PdfReader(pdf.toString()))) {
                assertTrue(documento.getNumberOfPages() > 0);
                for (int pagina = 1; pagina <= documento.getNumberOfPages(); pagina++) {
                    texto.append(PdfTextExtractor.getTextFromPage(documento.getPage(pagina))).append('\n');
                }
            }
            String contenido = texto.toString();
            assertTrue(contenido.contains(comentario), "Falta el comentario de la dieta en el PDF");
            assertTrue(contenido.contains(prefijo + "PDF37"), "Falta el nombre del paciente en el PDF");
            assertTrue(contenido.contains("Nombre:"), "Faltan los datos del paciente");
            assertTrue(contenido.contains("Comida") || contenido.contains("Desayuno")
                    || contenido.contains("Almuerzo") || contenido.contains("Cena"),
                    "No se encontró información del menú en el PDF");
        } finally {
            Files.deleteIfExists(pdf);
            if (respaldo != null) {
                Files.move(respaldo, pdf, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private int indiceDieta(int id) {
        List<DietSummary> actuales = filas();
        for (int i = 0; i < actuales.size(); i++) {
            if (actuales.get(i).getDietId() == id) return i;
        }
        return -1;
    }
}
