package com.javafx.nutrimaker;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.qa.TC;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/** Pruebas de aceptación ATS-01 a ATS-10: escenarios del nutriólogo de principio a fin desde JavaFX. */
public class AcceptanceTest extends NutriMakerGuiTest {

    private Stage ventana(String titulo) {
        return Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.isShowing() && titulo.equals(s.getTitle()))
                .map(w -> (Stage) w).findFirst().orElse(null);
    }

    private Set<Integer> idsEnLista() {
        return filas().stream().map(DietSummary::getDietId).collect(Collectors.toSet());
    }

    /** Desde el almacenamiento: Nueva, captura el paciente y llega a la creación de dieta. */
    private void registrarPaciente(String nombre) {
        pulsar("#createButton");
        esperar("el formulario de paciente", () -> visible("#nameTextField"));
        escribir("#nameTextField", nombre);
        escribir("#ageTextField", "29");
        escribir("#weightTextField", "64.5");
        escribir("#heightTextField", "1.68");
        pulsar("#saveButton");
        esperar("la pantalla de creación de dieta", () -> visible("#calTextField"));
    }

    private void crearDieta(String kcal, String comidas, String descanso, String comentario) {
        escribir("#calTextField", kcal);
        elegir("#quantityFoodDistribution", comidas);
        elegir("#selectFreeDay", descanso);
        escribir("#commentTextField", comentario);
        pulsar("#saveButton");
    }

    // ==========================================
    // PRUEBA ATS-01 (Iniciar sesión)
    // ==========================================
    /**
     * @objetivo Verificar desde la perspectiva del usuario que accede con su cuenta.
     * @precondicion Dado un usuario registrado con una dieta y otro usuario con dietas propias.
     * @pasos 1.- Cuando abre la aplicación e inicia sesión con sus datos; 2.- Y revisa la pantalla principal.
     * @esperado Entonces accede al almacenamiento de dietas; y ve solo su dieta, no las del otro usuario.
     */
    @TC(id = "ATS-01", rf = "RF-01", modulo = "login")
    @Test
    void ATS_01_usuarioRegistradoIniciaSesion() throws Exception {
        String email = crearUsuario();
        String otro = crearUsuario();
        List<Integer> propias = crearDietas(email, crearPaciente("Propio"), 1);
        List<Integer> ajenas = crearDietas(otro, crearPaciente("Ajeno"), 3);

        assertEquals("Login", titulo());
        iniciarSesion(email, PASSWORD);

        esperar("su dieta en la lista", () -> filas().size() == 1);
        assertEquals(Set.copyOf(propias), idsEnLista());
        assertTrue(idsEnLista().stream().noneMatch(ajenas::contains), "Ve dietas de otro usuario");
    }

    // ==========================================
    // PRUEBA ATS-02 (Crear una cuenta)
    // ==========================================
    /**
     * @objetivo Verificar que alguien sin cuenta pueda registrarse y entrar.
     * @precondicion Dado alguien sin cuenta en nutrimaker_test.
     * @pasos 1.- Cuando pulsa Registrarse y captura un correo y una contraseña válidos con su confirmación;
     * 2.- Y luego regresa a Iniciar sesión y entra con esa cuenta.
     * @esperado Entonces ve el mensaje de registro completado; y accede al almacenamiento de dietas, vacío.
     */
    @TC(id = "ATS-02", rf = "RF-02", modulo = "registro")
    @Test
    void ATS_02_usuarioNuevoCreaCuenta() {
        String email = "qa-ui-alta-" + prefijo.substring(6, 14) + "@nutri.test";
        pulsar("#signupButton");
        esperarPantalla("Register");

        escribir("#emailTextField", email);
        escribir("#passwordTextField", PASSWORD);
        escribir("#confPasswordTextField", PASSWORD);
        pulsar("#signupButton");
        esperar("el alta en MySQL", () -> new com.javafx.nutrimaker.repository.UserRepository().getIdByEmail(email) != null);
        registrarUsuario(email);
        esperar("el mensaje de registro completado", () -> robot.lookup(hasText("Registro completado")).tryQuery().isPresent());
        pulsar(robot.lookup("#closeButton").queryButton());

        pulsar("#loginButton");
        esperarPantalla("Login");
        iniciarSesion(email, PASSWORD);
        assertTrue(filas().isEmpty(), "Una cuenta nueva no debe tener dietas");
    }

    // ==========================================
    // PRUEBA ATS-03 (Registrar un paciente)
    // ==========================================
    /**
     * @objetivo Verificar el registro de un paciente por parte del nutriólogo.
     * @precondicion Dado un usuario con sesión iniciada.
     * @pasos 1.- Cuando pulsa Nueva y captura nombre, edad, peso y altura del paciente; 2.- Y pulsa Guardar.
     * @esperado Entonces el sistema acepta los datos y guarda al paciente; y continúa con la creación de la
     * dieta de ese paciente.
     */
    @TC(id = "ATS-03", rf = "RF-05", modulo = "pacientes")
    @Test
    void ATS_03_usuarioRegistraPaciente() throws Exception {
        iniciarSesion(crearUsuario(), PASSWORD);
        String nombre = prefijo + "María Ruiz";
        registrarPaciente(nombre);

        assertEquals(1, numero("SELECT COUNT(*) AS n FROM PACIENTE WHERE NOMBRE=? AND EDAD=29", nombre));
        assertTrue(visible("#quantityFoodDistribution") && visible("#selectFreeDay"),
                "No se muestra la creación de dieta");
    }

    // ==========================================
    // PRUEBA ATS-04 (Generar una dieta personalizada)
    // ==========================================
    /**
     * @objetivo Verificar que el nutriólogo obtenga una dieta completa con sus parámetros.
     * @precondicion Dado un usuario con sesión iniciada y un paciente registrado.
     * @pasos 1.- Cuando captura 2000 kcal, 4 comidas y el miércoles de descanso, y crea la dieta; 2.- Y la consulta en su lista.
     * @esperado Entonces recibe el aviso de dieta creada; y la dieta aparece en su lista con 4 comidas en cada
     * uno de los 6 días activos, el miércoles vacío y cada día dentro de 2000 kcal +/-10%.
     */
    @TC(id = "ATS-04", rf = "RF-04", modulo = "crear-dieta")
    @Test
    void ATS_04_usuarioGeneraDietaPersonalizada() throws Exception {
        iniciarSesion(crearUsuario(), PASSWORD);
        String paciente = prefijo + "Jorge Díaz";
        registrarPaciente(paciente);
        crearDieta("2000", "4", "Miercoles", "Dieta QA ATS-04");

        esperarAviso("¡La dieta se ha creado con exito!");
        esperarPantalla("Almacenamiento de Dietas");
        esperar("la dieta en la lista", () -> filas().size() == 1);
        int dieta = filas().get(0).getDietId();
        assertEquals(paciente, filas().get(0).getPatientName());
        for (int dia = 1; dia <= 7; dia++) {
            assertEquals(dia == 3 ? 0 : 4,
                    numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=? AND DIA_SEMANA=?", dieta, dia),
                    "Cantidad de comidas incorrecta el día " + dia);
        }
        assertEquals(0, numero("""
                SELECT COUNT(*) AS n FROM (
                    SELECT SUM(CALORIAS) AS total FROM COMIDA WHERE ID_DIETA=? GROUP BY DIA_SEMANA
                    HAVING total < 1800 OR total > 2200) AS fuera
                """, dieta), "Algún día quedó fuera de 2000 kcal +/-10%");
        cerrarAviso();
    }

    // ==========================================
    // PRUEBA ATS-05 (Sin comidas compatibles)
    // ==========================================
    /**
     * @objetivo Verificar que el sistema avise con claridad cuando no puede generar una dieta.
     * @precondicion Dado un usuario con sesión iniciada y un paciente registrado; ninguna combinación del
     * catálogo cumple 300 kcal repartidas en 6 comidas.
     * @pasos 1.- Cuando captura 300 kcal, 6 comidas y un día de descanso; 2.- Y pulsa Guardar.
     * @esperado Entonces el sistema muestra un mensaje de que no se pudo crear la dieta; y no guarda una dieta
     * incompleta ni se cierra la aplicación.
     */
    @TC(id = "ATS-05", rf = "RF-04", modulo = "crear-dieta")
    @Test
    void ATS_05_avisaCuandoNoHayComidasCompatibles() throws Exception {
        String email = crearUsuario();
        iniciarSesion(email, PASSWORD);
        registrarPaciente(prefijo + "Sin Opciones");
        crearDieta("300", "6", "Domingo", "Dieta QA ATS-05");

        esperarAviso("No se pudo crear la dieta");
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM DIETA WHERE ID_USUARIO=?", idDe(email)),
                "Se guardó una dieta incompleta");
        assertTrue(stage.isShowing(), "La aplicación se cerró");
        assertTrue(visible("#calTextField"), "Debe permanecer en la creación de dieta para corregir los datos");
        cerrarAviso();
    }

    // ==========================================
    // PRUEBA ATS-06 (Consultar sus dietas)
    // ==========================================
    /**
     * @objetivo Verificar que el nutriólogo encuentre y revise sus dietas.
     * @precondicion Dado un usuario con 12 dietas y otro usuario con 5.
     * @pasos 1.- Cuando inicia sesión y revisa la lista; 2.- Y navega a la siguiente página.
     * @esperado Entonces ve 10 dietas con paciente y fecha y luego las 2 restantes; y nunca ve las del otro usuario.
     */
    @TC(id = "ATS-06", rf = "RF-03", modulo = "dietas-almacenadas")
    @Test
    void ATS_06_usuarioConsultaSusDietas() throws Exception {
        String email = crearUsuario();
        String otro = crearUsuario();
        List<Integer> propias = crearDietas(email, crearPaciente("Consulta"), 12);
        List<Integer> ajenas = crearDietas(otro, crearPaciente("Otro"), 5);

        iniciarSesion(email, PASSWORD);
        esperar("la primera página", () -> filas().size() == 10);
        Set<Integer> vistas = new java.util.HashSet<>(idsEnLista());
        filas().forEach(d -> assertTrue(d.getPatientName().endsWith("Consulta") && d.getCreationDate() != null));
        pulsar("#nextButton");
        esperar("la segunda página", () -> filas().size() == 2);
        vistas.addAll(idsEnLista());

        assertEquals(Set.copyOf(propias), vistas);
        assertTrue(vistas.stream().noneMatch(ajenas::contains), "Ve dietas de otro usuario");
    }

    // ==========================================
    // PRUEBA ATS-07 (Modificar una dieta)
    // ==========================================
    /**
     * @objetivo Verificar que el nutriólogo ajuste una dieta ya creada.
     * @precondicion Dado un usuario con una dieta generada.
     * @pasos 1.- Cuando la abre para editar, cambia el comentario y las calorías y guarda;
     * 2.- Y vuelve a abrirla para consultarla.
     * @esperado Entonces el sistema le notifica que los cambios se guardaron y regresa al almacenamiento;
     * y la dieta muestra el comentario y las calorías nuevos.
     */
    @TC(id = "ATS-07", rf = "RF-06", modulo = "modificar-dieta")
    @Test
    void ATS_07_usuarioModificaDieta() throws Exception {
        String email = crearUsuario();
        int dieta = generarDieta(email, crearPaciente("Editar"), "Comentario original");
        iniciarSesion(email, PASSWORD);
        esperar("la dieta en la lista", () -> filas().size() == 1);

        pulsar(accion(0, "edit"));
        esperar("Editar dieta", () -> ventana("Editar Dieta") != null);
        Node editar = ventana("Editar Dieta").getScene().getRoot();
        TextField comentario = robot.from(editar).lookup("#commentTextField").queryAs(TextField.class);
        TextField calorias = robot.from(editar).lookup("#calTextField").queryAs(TextField.class);
        robot.interact(() -> {
            comentario.setText("Comentario editado ATS-07");
            calorias.setText("2100");
        });
        pulsar(robot.from(editar).lookup("#saveButton").queryButton());
        esperar("cerrar Editar dieta", () -> ventana("Editar Dieta") == null);
        // SRS 1.11.3.4: si la modificación es exitosa, el sistema notifica al usuario y regresa a la principal.
        boolean notificado = Window.getWindows().stream().anyMatch(w -> w.isShowing() && w != stage);
        robot.interact(() -> new java.util.ArrayList<>(Window.getWindows()).stream()
                .filter(w -> w != stage).forEach(Window::hide));

        assertEquals("Comentario editado ATS-07",
                db.read(c -> com.javafx.nutrimaker.database.DatabaseClient.query(c,
                        "SELECT COMENTARIO AS n FROM DIETA WHERE ID_DIETA=?", dieta)
                        .get(0).getAsJsonObject().get("n").getAsString()));
        pulsar(accion(0, "edit"));
        esperar("Editar dieta", () -> ventana("Editar Dieta") != null);
        Node reabierta = ventana("Editar Dieta").getScene().getRoot();
        assertEquals("Comentario editado ATS-07",
                robot.from(reabierta).lookup("#commentTextField").queryAs(TextField.class).getText());
        assertEquals(2100.0, Double.parseDouble(
                robot.from(reabierta).lookup("#calTextField").queryAs(TextField.class).getText()), 0.001);
        pulsar(robot.from(reabierta).lookup("#cancelButton").queryButton());
        assertTrue(notificado, "No se notificó al usuario que los cambios se guardaron");
    }

    // ==========================================
    // PRUEBA ATS-08 (Cancelar la eliminación)
    // ==========================================
    /**
     * @objetivo Verificar que cancelar no borre la dieta.
     * @precondicion Dado un usuario con una dieta existente.
     * @pasos 1.- Cuando pulsa eliminar en la dieta; 2.- Y pulsa Cancelar en la confirmación.
     * @esperado Entonces el diálogo se cierra; y la dieta sigue en la lista y en la base de datos.
     */
    @TC(id = "ATS-08", rf = "RF-07", modulo = "eliminar-dieta")
    @Test
    void ATS_08_usuarioCancelaEliminacion() throws Exception {
        String email = crearUsuario();
        int dieta = crearDietas(email, crearPaciente("Cancelar"), 1).get(0);
        iniciarSesion(email, PASSWORD);
        esperar("la dieta en la lista", () -> filas().size() == 1);

        pulsar(accion(0, "delete"));
        esperar("la confirmación", () -> robot.lookup(hasText("¿Desea Eliminar")).tryQuery().isPresent());
        Button cancelar = robot.lookup("#cancelBtn").queryButton();
        pulsar(cancelar);

        esperar("cerrar la confirmación", () -> !cancelar.getScene().getWindow().isShowing());
        assertEquals(Set.of(dieta), idsEnLista());
        assertEquals(1, numero("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=?", dieta));
    }

    // ==========================================
    // PRUEBA ATS-09 (Confirmar la eliminación)
    // ==========================================
    /**
     * @objetivo Verificar que confirmar elimine la dieta.
     * @precondicion Dado un usuario con dos dietas generadas.
     * @pasos 1.- Cuando pulsa eliminar en una dieta; 2.- Y pulsa Aceptar en la confirmación.
     * @esperado Entonces la dieta desaparece de la lista; y ya no existe en la base de datos ni sus comidas;
     * la otra dieta permanece.
     */
    @TC(id = "ATS-09", rf = "RF-07", modulo = "eliminar-dieta")
    @Test
    void ATS_09_usuarioConfirmaEliminacion() throws Exception {
        String email = crearUsuario();
        int paciente = crearPaciente("Eliminar");
        int conservar = generarDieta(email, paciente, "QA-UI conservar");
        int eliminar = generarDieta(email, paciente, "QA-UI eliminar");
        iniciarSesion(email, PASSWORD);
        esperar("las dos dietas", () -> filas().size() == 2);
        int fila = filas().get(0).getDietId() == eliminar ? 0 : 1;

        pulsar(accion(fila, "delete"));
        esperar("la confirmación", () -> robot.lookup(hasText("¿Desea Eliminar")).tryQuery().isPresent());
        pulsar(robot.lookup("#acceptBtn").queryButton());

        esperar("que desaparezca de la lista", () -> filas().size() == 1);
        assertEquals(Set.of(conservar), idsEnLista());
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM DIETA WHERE ID_DIETA=?", eliminar));
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", eliminar));
        assertEquals(18, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", conservar));
    }

    // ==========================================
    // PRUEBA ATS-10 (Generar y consultar el PDF)
    // ==========================================
    /**
     * @objetivo Verificar que el nutriólogo obtenga el PDF de una dieta para entregarlo.
     * @precondicion Dado un usuario con una dieta generada con comentario; carpeta Documents disponible.
     * @pasos 1.- Cuando pulsa PDF en la dieta; 2.- Y abre el archivo generado.
     * @esperado Entonces se genera el PDF; y contiene los datos del paciente, el comentario, el menú semanal
     * y los nutrimentos.
     */
    @TC(id = "ATS-10", rf = "RF-08", modulo = "exportacion")
    @Test
    void ATS_10_usuarioGeneraPdfDeSuDieta() throws Exception {
        String email = crearUsuario();
        int paciente = crearPaciente("PDF");
        generarDieta(email, paciente, "Tomar 2 litros de agua al dia");
        String comida = db.read(c -> com.javafx.nutrimaker.database.DatabaseClient.query(c,
                "SELECT NOMBRE_COMIDA AS n FROM COMIDA c JOIN DIETA d ON d.ID_DIETA=c.ID_DIETA WHERE d.ID_USUARIO=? LIMIT 1",
                idDe(email)).get(0).getAsJsonObject().get("n").getAsString());
        iniciarSesion(email, PASSWORD);
        esperar("la dieta en la lista", () -> filas().size() == 1);

        // PDFBuilder escribe siempre en ~/Documents/diet.pdf: se respalda y restaura el archivo del usuario.
        Field campo = PDFBuilder.class.getDeclaredField("PATH");
        campo.setAccessible(true);
        Path pdf = Path.of((String) campo.get(null));
        Files.createDirectories(pdf.getParent());
        Path respaldo = Files.exists(pdf) ? Files.move(pdf, Files.createTempFile("diet-backup", ".pdf"),
                StandardCopyOption.REPLACE_EXISTING) : null;
        try {
            // Tras escribir el PDF, exportPdf intenta abrir el visor del sistema; sin escritorio lanza
            // HeadlessException en el hilo de JavaFX y TestFX la relanzaría en la siguiente espera.
            WaitForAsyncUtils.autoCheckException = false;
            try {
                pulsar(accion(0, "pdf"));
                esperar("el archivo PDF", () -> Files.exists(pdf) && pdf.toFile().length() > 0);
            } finally {
                WaitForAsyncUtils.clearExceptions();
                WaitForAsyncUtils.autoCheckException = true;
            }

            StringBuilder texto = new StringBuilder();
            try (PdfDocument doc = new PdfDocument(new PdfReader(pdf.toString()))) {
                for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                    texto.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
                }
            }
            String contenido = texto.toString();
            assertTrue(contenido.contains(prefijo + "PDF"), "Falta el nombre del paciente");
            assertTrue(contenido.contains("Edad: 30") && contenido.contains("Peso: 70.0"), "Faltan los datos del paciente");
            assertTrue(contenido.contains("Tomar 2 litros de agua al dia"), "Falta el comentario");
            assertTrue(contenido.contains("LUNES") && contenido.contains("SÁBADO"), "Falta el menú semanal");
            assertTrue(contenido.contains(comida), "Faltan las comidas");
            assertTrue(contenido.contains("Calorias") && contenido.contains("Proteinas"), "Faltan los nutrimentos");
        } finally {
            Files.deleteIfExists(pdf);
            if (respaldo != null) Files.move(respaldo, pdf);
        }
    }
}
