package com.javafx.nutrimaker;

import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.qa.TC;
import com.javafx.nutrimaker.repository.UserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/** Pruebas de sistema de acceso y registro (#35), con JavaFX real y MySQL de pruebas. */
public class AccessRegistrationSystemTest extends ApplicationTest {

    private static final String PASSWORD = "Prueba1234";
    private static final String TEST_URL =
            "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final DatabaseClient DB = new DatabaseClient();
    private final UserRepository users = new UserRepository();
    private String temporaryEmail;

    @BeforeAll
    static void configureTestDatabase() throws Exception {
        // No depender de las variables de PowerShell: NetBeans puede no heredarlas.
        System.setProperty("nutrimaker.db.url", TEST_URL);
        System.setProperty("nutrimaker.db.user",
                System.getenv().getOrDefault("NUTRIMAKER_DB_USER", "root"));
        System.setProperty("nutrimaker.db.password",
                System.getenv().getOrDefault("NUTRIMAKER_DB_PASSWORD", ""));
        assertTestDatabase();
    }

    private static void assertTestDatabase() throws Exception {
        try (Connection connection = DB.connect()) {
            assertEquals("nutrimaker_test", connection.getCatalog(),
                    "SEGURIDAD: las pruebas solo pueden ejecutarse en nutrimaker_test");
        }
    }

    @Start
    public void start(Stage stage) throws Exception {
        assertTestDatabase();
        Parent root = FXMLLoader.load(Main.class.getResource("LoginForm.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
        stage.toFront();
    }

    private String newEmail() {
        return "qa" + UUID.randomUUID().toString().substring(0, 10).replace("-", "") + "@x.mx";
    }

    private void prepareUser() throws Exception {
        assertTestDatabase();
        temporaryEmail = newEmail();
        assertTrue(users.insertUser(temporaryEmail, PASSWORD), "No se pudo crear usuario de prueba");
        assertNotNull(users.getIdByEmail(temporaryEmail));
    }

    private void fillLogin(FxRobot robot, String email, String password) {
        robot.clickOn("#emailTextField").write(email);
        robot.clickOn("#passwordTextField").write(password);
    }

    private void fillRegistration(FxRobot robot, String email, String password, String confirmation) {
        robot.clickOn("#emailTextField").write(email);
        robot.clickOn("#passwordTextField").write(password);
        robot.clickOn("#confPasswordTextField").write(confirmation);
    }

    private boolean visibleText(String expected) {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> window.getScene() != null)
                .anyMatch(window -> window.getScene().getRoot().lookupAll(".label").stream()
                        .anyMatch(node -> node instanceof javafx.scene.control.Label label
                                && label.getText() != null && label.getText().contains(expected)));
    }

    private void assertLoginStillVisible() {
        assertTrue(Window.getWindows().stream().filter(Window::isShowing)
                .anyMatch(window -> window.getScene() != null
                        && window.getScene().getRoot().lookup("#loginButton") != null),
                "El formulario de inicio de sesión debe permanecer abierto");
    }

    // ==========================================
    // PRUEBA STS-01 (ACCESO CORRECTO)
    // ==========================================
    /**
     * @objetivo Verificar que un usuario registrado acceda a almacenamiento de dietas desde JavaFX.
     * @precondicion Base nutrimaker_test disponible y usuario temporal registrado.
     * @pasos 1.- Abrir LoginForm; 2.- Escribir credenciales válidas; 3.- Pulsar Iniciar Sesión; 4.- Verificar pantalla de dietas.
     * @esperado La ventana muestra Almacenamiento de Dietas y su tabla de dietas.
     */
    @TC(id="STS-01", rf="RF-01", modulo="login")
    @Test
    void STS_01_inicioSesionCorrecto() throws Exception {
        prepareUser();
        fillLogin(this, temporaryEmail, PASSWORD);
        clickOn("#loginButton");
        waitForFxEvents();
        assertNotNull(lookup("#dietsTable").tryQuery().orElse(null),
                "El inicio de sesión válido debe abrir el almacenamiento de dietas");
    }

    // ==========================================
    // PRUEBA STS-02 (CREDENCIALES INCORRECTAS)
    // ==========================================
    /**
     * @objetivo Verificar que credenciales incorrectas muestren una advertencia sin cerrar el login.
     * @precondicion Formulario de acceso abierto y usuario temporal registrado.
     * @pasos 1.- Introducir correo válido y contraseña errónea; 2.- Pulsar Iniciar Sesión; 3.- Comprobar mensaje y permanencia del formulario.
     * @esperado Se muestra un diálogo de credenciales incorrectas y el login permanece abierto.
     */
    @TC(id="STS-02", rf="RF-01", modulo="login")
    @Test
    void STS_02_rechazarCredencialesInvalidas() throws Exception {
        prepareUser();
        fillLogin(this, temporaryEmail, "Incorrecta1");
        clickOn("#loginButton");
        waitForFxEvents();
        assertLoginStillVisible();
        assertTrue(Window.getWindows().stream().filter(Window::isShowing).count() >= 2,
                "Las credenciales incorrectas deben mostrar una ventana de advertencia");
    }

    // ==========================================
    // PRUEBA STS-03 (REGISTRO Y CONFIRMACIÓN)
    // ==========================================
    /**
     * @objetivo Verificar el registro desde JavaFX y la confirmación visible al usuario.
     * @precondicion Base nutrimaker_test disponible y correo temporal no registrado.
     * @pasos 1.- Abrir Registrarse; 2.- Escribir correo y contraseñas válidas; 3.- Confirmar registro; 4.- Comprobar diálogo y usuario persistido.
     * @esperado El usuario queda registrado y aparece el mensaje Registro completado sin cerrar la aplicación.
     */
    @TC(id="STS-03", rf="RF-02", modulo="registro")
    @Test
    void STS_03_registroConConfirmacion() throws Exception {
        assertTestDatabase();
        temporaryEmail = newEmail();
        clickOn("#signupButton");
        waitForFxEvents();
        assertNotNull(lookup("#confPasswordTextField").tryQuery().orElse(null),
                "Debe mostrarse el formulario de registro");
        fillRegistration(this, temporaryEmail, PASSWORD, PASSWORD);
        clickOn("#signupButton");
        waitForFxEvents();
        assertNotNull(users.getIdByEmail(temporaryEmail), "El registro debe persistirse en la base de pruebas");
        assertTrue(visibleText("Registro completado"),
                "El usuario debe ver la confirmación Registro completado");
    }

    @AfterEach
    void cleanUp() throws Exception {
        try {
            if (temporaryEmail != null) {
                assertTestDatabase();
                try (Connection connection = DB.connect();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM USUARIO WHERE CORREO = ?")) {
                    statement.setString(1, temporaryEmail);
                    statement.executeUpdate();
                }
            }
        } finally {
            // TestFX inicia una nueva escena para cada prueba; cerrar los diálogos secundarios.
            javafx.application.Platform.runLater(() -> {
                // Crear una copia: hide() modifica Window.getWindows() durante el recorrido.
                for (Window window : new java.util.ArrayList<>(Window.getWindows())) {
                    if (window.isShowing() && window instanceof Stage stage
                            && !"Login".equals(stage.getTitle())
                            && !"Register".equals(stage.getTitle())
                            && !"Almacenamiento de Dietas".equals(stage.getTitle())) {
                        window.hide();
                    }
                }
            });
        }
    }
}
