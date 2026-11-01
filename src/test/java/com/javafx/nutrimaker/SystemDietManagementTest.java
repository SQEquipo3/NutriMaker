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
     * @nombre Modificar una dieta desde la interfaz
     * @objetivo Verificar la edición de una dieta desde la interfaz: el botón Editar abre el diálogo «Editar
     * Dieta» (EditDietController) y, al pulsar Guardar, save() guarda los cambios con
     * DietRepository.updateDietForUser() y cierra el diálogo (RF-06; URS_25).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario con 11 dietas (dos páginas) y
     * sesión iniciada.
     * @pasos 1.- En la tabla de dietas pulsar Editar en la primera fila;
     * 2.- Cambiar las calorías a 2100 y escribir un comentario nuevo;
     * 3.- Pulsar Guardar y esperar a que se cierre el editor;
     * 4.- Consultar en MySQL la fila de DIETA con ese id.
     * @esperado El diálogo se cierra y la dieta queda en MySQL con 2100 kcal y el comentario nuevo.
     */
    @Test
    @TC(id = "STS-07", rf = "RF-06", modulo = "modificar-dieta")
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
     * @nombre Eliminar una dieta con confirmación
     * @objetivo Verificar la eliminación de una dieta desde la interfaz: el botón Eliminar abre la confirmación
     * (AskDialogController) y, al pulsar Aceptar, DietStorageController borra la dieta con
     * DietRepository.deleteDiet() y la quita de la tabla (RF-07; URS_29: confirmar antes de
     * eliminar).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario con 11 dietas y sesión iniciada.
     * @pasos 1.- En la tabla de dietas pulsar Eliminar en la primera fila;
     * 2.- Pulsar Aceptar (#acceptBtn) en el diálogo de confirmación;
     * 3.- Consultar en MySQL la fila de DIETA con ese id;
     * 4.- Revisar que la dieta ya no aparezca en la tabla.
     * @esperado La dieta desaparece de la tabla y de la base de datos.
     */
    @Test
    @TC(id = "STS-08", rf = "RF-07", modulo = "eliminar-dieta")
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
     * @nombre Exportar una dieta a PDF desde la interfaz
     * @objetivo Verificar la exportación a PDF desde la interfaz: el botón PDF de una fila llama
     * DietRepository.getDietObjectById() y PDFBuilder.exportPdf(), que escribe ~/Documents/diet.pdf
     * con los datos de la dieta (RF-08; URS_31).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario con 11 dietas, una de ellas
     * generada con menú y comentario. Si ya existe ~/Documents/diet.pdf se respalda y se restaura
     * al final.
     * @pasos 1.- Iniciar sesión y ubicar la dieta en la tabla (página 1 o 2);
     * 2.- Pulsar el botón PDF de esa fila;
     * 3.- Esperar a que se cree Documents/diet.pdf;
     * 4.- Extraer el texto de sus páginas.
     * @esperado El PDF contiene el nombre del paciente, «Nombre:», el comentario y al menos una comida del
     * menú.
     * @nota La inspección visual del formato puede registrarse adicionalmente como evidencia manual.
     */
    @Test
    @TC(id = "STS-09", rf = "RF-08", modulo = "exportacion")
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
