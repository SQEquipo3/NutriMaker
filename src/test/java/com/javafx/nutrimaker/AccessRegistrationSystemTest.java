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
     * @nombre Inicio de sesión correcto desde la interfaz
     * @objetivo Verificar el inicio de sesión de punta a punta desde la interfaz: en LoginForm.fxml, al pulsar
     * «Iniciar sesión», LoginController.login() valida los campos, autentica con
     * UserRepository.verifyPasswordByEmail() y abre la pantalla «Almacenamiento de Dietas» (RF-01;
     * URS_05: solo usuarios registrados).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario temporal registrado con
     * UserRepository.insertUser() y la contraseña «Prueba1234».
     * @pasos 1.- Abrir LoginForm.fxml;
     * 2.- Escribir el correo y la contraseña en #emailTextField y #passwordTextField;
     * 3.- Pulsar #loginButton;
     * 4.- Buscar la tabla #dietsTable.
     * @esperado Se abre «Almacenamiento de Dietas» y se muestra su tabla de dietas.
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
     * @nombre Credenciales incorrectas desde la interfaz
     * @objetivo Verificar que credenciales incorrectas no den acceso desde la interfaz: LoginController.login()
     * llama invalidCredentials(), que abre una ventana de advertencia, y el formulario de login sigue
     * abierto (RF-01; URS_04: notificar credenciales incorrectas).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario temporal registrado.
     * @pasos 1.- Escribir el correo del usuario y la contraseña «Incorrecta1»;
     * 2.- Pulsar #loginButton;
     * 3.- Contar las ventanas abiertas y buscar #loginButton.
     * @esperado Aparece una segunda ventana con la advertencia y el formulario de login sigue visible.
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
     * @nombre Registro con confirmación desde la interfaz
     * @objetivo Verificar el registro de un usuario desde la interfaz: en el formulario «Registrarse»,
     * SignUpFormController.signUp() valida los datos, guarda con UserRepository.insertUser() y
     * muestra la confirmación «Registro completado» (RF-02; URS_09 y URS_43).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Correo temporal que no existe en
     * USUARIO.
     * @pasos 1.- Pulsar #signupButton en el login para abrir el registro;
     * 2.- Escribir correo, contraseña «Prueba1234» y su confirmación;
     * 3.- Pulsar #signupButton;
     * 4.- Buscar el usuario con UserRepository.getIdByEmail() y el texto «Registro completado» en
     * pantalla.
     * @esperado El usuario queda guardado en nutrimaker_test y se muestra «Registro completado» sin cerrar la
     * aplicación.
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
