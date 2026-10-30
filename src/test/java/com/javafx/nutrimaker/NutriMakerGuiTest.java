package com.javafx.nutrimaker;

import com.google.gson.JsonParser;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.repository.DietRepository;
import com.javafx.nutrimaker.repository.UserRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static com.javafx.nutrimaker.database.DatabaseClient.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base de las pruebas de sistema (STS) y aceptación (ATS): abre NutriMaker en la pantalla de inicio de sesión
 * contra nutrimaker_test y la maneja con TestFX. En CI corre con Xvfb a 1920x1080.
 * Los textos se capturan con setText (no dependen de la distribución del teclado) y los botones se disparan con fire().
 */
public abstract class NutriMakerGuiTest {
    protected static final String DB_NAME = "nutrimaker_test";
    private static final String TEST_URL = "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    protected static final String PASSWORD = "Temporal123!";
    private static String previousUrl;

    protected final DatabaseClient db = new DatabaseClient();
    protected final FxRobot robot = new FxRobot();
    protected Stage stage;
    /** Prefijo de los pacientes creados por la prueba, para limpiarlos aunque se capturen desde la interfaz. */
    protected final String prefijo = "QA-UI-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    private final List<Integer> usuariosCreados = new ArrayList<>();

    @BeforeAll
    static void abrirToolkitYBaseDePruebas() throws Exception {
        System.setProperty("testfx.robot", "glass");
        // Sin escritorio, PDFBuilder.exportPdf falla al abrir el visor en vez de bloquear el hilo de JavaFX
        System.setProperty("java.awt.headless", "true");
        previousUrl = System.getProperty("nutrimaker.db.url");
        System.setProperty("nutrimaker.db.url", TEST_URL);
        try (Connection c = new DatabaseClient().connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "PROHIBIDO ejecutar contra la base principal");
        }
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Otra prueba ya inició JavaFX
        }
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void restaurarConexion() {
        if (previousUrl == null) System.clearProperty("nutrimaker.db.url");
        else System.setProperty("nutrimaker.db.url", previousUrl);
    }

    /** Abre la aplicación como Main.start: la pantalla de inicio de sesión. */
    @BeforeEach
    void abrirAplicacion() throws Exception {
        // Ninguna ventana de una prueba anterior debe seguir abierta
        robot.interact(() -> new ArrayList<>(Window.getWindows()).forEach(Window::hide));
        stage = WaitForAsyncUtils.asyncFx(() -> {
            Stage s = new Stage();
            Parent root = new FXMLLoader(Main.class.getResource("LoginForm.fxml")).load();
            s.setScene(new Scene(root));
            s.setTitle("Login");
            s.setX(0);
            s.setY(0);
            s.show();
            s.toFront();
            return s;
        }).get(30, TimeUnit.SECONDS);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void cerrarAplicacionYLimpiar() throws Exception {
        robot.interact(() -> new ArrayList<>(Window.getWindows()).forEach(Window::hide));
        try (Connection c = db.connect()) {
            assertEquals(DB_NAME, c.getCatalog(), "Se detectó otra base de datos");
        }
        db.transaction(c -> {
            for (int id : usuariosCreados) {
                execute(c, "DELETE ic FROM INGREDIENTE_COMIDA ic JOIN COMIDA co ON co.ID_COMIDA=ic.ID_COMIDA JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", id);
                execute(c, "DELETE co FROM COMIDA co JOIN DIETA d ON d.ID_DIETA=co.ID_DIETA WHERE d.ID_USUARIO=?", id);
                execute(c, "DELETE FROM DIETA WHERE ID_USUARIO=?", id);
                execute(c, "DELETE FROM USUARIO WHERE ID_USUARIO=?", id);
            }
            execute(c, "DELETE p FROM PACIENTE p LEFT JOIN DIETA d ON d.ID_PACIENTE=p.ID_PACIENTE " +
                    "WHERE p.NOMBRE LIKE ? AND d.ID_DIETA IS NULL", prefijo + "%");
            return null;
        });
    }

    // ------------------------------------------------------------------ datos de prueba

    /** Registra un usuario en nutrimaker_test y lo limpia al final. */
    protected String crearUsuario() {
        String email = "qa-ui-" + UUID.randomUUID().toString().substring(0, 8) + "@nutri.test";
        assertTrue(new UserRepository().insertUser(email, PASSWORD), "No se pudo crear el usuario de prueba");
        registrarUsuario(email);
        return email;
    }

    /** Marca para limpieza un usuario creado desde la interfaz. */
    protected int registrarUsuario(String email) {
        Integer id = new UserRepository().getIdByEmail(email);
        assertNotNull(id, "No existe el usuario " + email);
        usuariosCreados.add(id);
        return id;
    }

    protected int idDe(String email) {
        return new UserRepository().getIdByEmail(email);
    }

    protected int crearPaciente(String nombre) throws Exception {
        return new com.javafx.nutrimaker.repository.PatientRepository()
                .createPatientAndGetId(prefijo + nombre, 30, 70.0, 1.70);
    }

    /** Crea dietas sin comidas directamente en MySQL (para listados y paginación). */
    protected List<Integer> crearDietas(String email, int pacienteId, int cantidad) throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("user_id", idDe(email));
            data.put("patient_id", pacienteId);
            data.put("calories", 2000);
            data.put("meals_per_day", 3);
            data.put("rest_day", "SUNDAY");
            data.put("note", "QA-UI dieta " + (i + 1));
            ids.add(JsonParser.parseString(new DietRepository().createDiet(data))
                    .getAsJsonObject().get("diet_id").getAsInt());
        }
        return ids;
    }

    /** Genera una dieta completa con el algoritmo de la aplicación (3 comidas, descanso en domingo). */
    protected int generarDieta(String email, int pacienteId, String comentario) throws Exception {
        assertTrue(new com.javafx.nutrimaker.repository.MealRepository()
                        .createNewDiet(2000, 3, "SUNDAY", idDe(email), pacienteId, comentario),
                "No se pudo generar la dieta de prueba");
        return numero("SELECT ID_DIETA AS n FROM DIETA WHERE ID_USUARIO=? AND COMENTARIO=?", idDe(email), comentario);
    }

    protected int numero(String sql, Object... args) throws Exception {
        return db.read(c -> query(c, sql, args).get(0).getAsJsonObject().get("n").getAsInt());
    }

    // ------------------------------------------------------------------ interfaz

    /** Búsquedas dentro de la ventana principal de esta prueba (los diálogos son otras ventanas). */
    protected org.testfx.service.query.NodeQuery enPrincipal(String selector) {
        return robot.from(stage.getScene().getRoot()).lookup(selector);
    }

    protected boolean visible(String selector) {
        return enPrincipal(selector).tryQuery().map(Node::isVisible).orElse(false);
    }

    protected void escribir(String selector, String texto) {
        TextInputControl campo = enPrincipal(selector).queryAs(TextInputControl.class);
        robot.interact(() -> campo.setText(texto));
    }

    protected void elegir(String selector, String opcion) {
        @SuppressWarnings("unchecked")
        ChoiceBox<String> choice = enPrincipal(selector).queryAs(ChoiceBox.class);
        robot.interact(() -> choice.setValue(opcion));
    }

    protected void pulsar(String selector) {
        pulsar(enPrincipal(selector).queryButton());
    }

    /** Dispara la acción del botón como un clic. Sin gestor de ventanas en Xvfb, el clic del mouse a veces se pierde. */
    protected void pulsar(Node boton) {
        assertTrue(boton.isVisible() && !boton.isDisabled(), "El botón no está disponible: " + boton);
        Platform.runLater(((Button) boton)::fire); // asíncrono: algunos botones abren diálogos modales
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void esperar(String motivo, BooleanSupplier condicion) {
        try {
            WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, condicion::getAsBoolean);
        } catch (Exception e) {
            fail("Tiempo agotado esperando: " + motivo + ". En pantalla: " + ventanasAbiertas());
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Título y textos de cada ventana abierta, para diagnosticar fallos. */
    protected String ventanasAbiertas() {
        StringBuilder sb = new StringBuilder();
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (!w.isShowing() || w.getScene() == null) continue;
            sb.append("[").append(w instanceof Stage s ? s.getTitle() : w.getClass().getSimpleName()).append(": ");
            robot.from(w.getScene().getRoot()).lookup((Node n) -> n instanceof Label).queryAllAs(Label.class)
                    .forEach(l -> sb.append("«").append(l.getText()).append("» "));
            sb.append("] ");
        }
        return sb.toString();
    }

    protected String titulo() {
        return stage.getTitle();
    }

    protected void esperarPantalla(String titulo) {
        esperar("la pantalla «" + titulo + "» (actual: «" + titulo() + "»)", () -> titulo.equals(titulo()));
    }

    /** Inicia sesión desde la pantalla de login y espera el almacenamiento de dietas. */
    protected void iniciarSesion(String email, String password) {
        escribir("#emailTextField", email);
        escribir("#passwordTextField", password);
        pulsar("#loginButton");
        esperarPantalla("Almacenamiento de Dietas");
    }

    /** Texto del diálogo de aviso (DialogInputs) que esté abierto, o null. */
    protected String mensajeDeAviso() {
        return robot.lookup("#warningLabel").tryQueryAs(Label.class).map(Label::getText).orElse(null);
    }

    protected void esperarAviso(String texto) {
        esperar("el aviso «" + texto + "» (actual: «" + mensajeDeAviso() + "»)",
                () -> mensajeDeAviso() != null && mensajeDeAviso().contains(texto));
    }

    protected void cerrarAviso() {
        pulsar(robot.lookup("#closeButton").queryButton());
    }

    @SuppressWarnings("unchecked")
    protected TableView<DietSummary> tabla() {
        return enPrincipal("#dietsTable").queryAs(TableView.class);
    }

    protected List<DietSummary> filas() {
        return new ArrayList<>(tabla().getItems());
    }

    /** Botón de acción de una fila de la tabla: pdf, clone, edit o delete. */
    protected Button accion(int fila, String icono) {
        robot.interact(() -> tabla().scrollTo(fila));
        return robot.from(tabla()).lookup((Node n) -> n instanceof Button b
                        && b.getGraphic() instanceof ImageView iv
                        && iv.getImage() != null && iv.getImage().getUrl().endsWith("/" + icono + ".png")
                        && filaDe(b) == fila)
                .queryButton();
    }

    private static int filaDe(Node nodo) {
        for (Node n = nodo; n != null; n = n.getParent()) {
            if (n instanceof TableCell<?, ?> cell) return cell.isEmpty() ? -1 : cell.getIndex();
        }
        return -1;
    }
}
