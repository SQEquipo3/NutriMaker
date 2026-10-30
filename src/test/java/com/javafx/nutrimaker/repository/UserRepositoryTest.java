package com.javafx.nutrimaker.repository;

import static com.javafx.nutrimaker.database.DatabaseClient.execute;
import static com.javafx.nutrimaker.database.DatabaseClient.nextId;
import static com.javafx.nutrimaker.database.DatabaseClient.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonArray;
import com.javafx.nutrimaker.database.DatabaseClient;
import com.javafx.nutrimaker.qa.TC;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

/*
 * Pruebas de integración de usuarios contra MySQL (ITS-01 a ITS-03).
 *
 * SOLO corren contra la base nutrimaker_test. Si la conexión apunta a otra base
 * (por ejemplo la de desarrollo, nutrimaker) o no hay MySQL, se SALTAN: nunca tocan datos reales.
 * Cada prueba usa correos únicos y los borra al terminar.
 */
public class UserRepositoryTest {

    private static final String BASE_DE_PRUEBAS = "nutrimaker_test";

    private final DatabaseClient db = new DatabaseClient();
    private final UserRepository users = new UserRepository();
    private final List<String> correosCreados = new ArrayList<>();

    // Conexión de pruebas: esta prueba le indica a DatabaseClient que use SOLO la base nutrimaker_test.
    private static final String URL_PRUEBAS =
            "jdbc:mysql://localhost:3306/nutrimaker_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USUARIO_PRUEBAS = "nutri_test";   // usuario sin contraseña, solo con acceso a nutrimaker_test
    private static final String PASSWORD_PRUEBAS = "";

    @BeforeAll
    static void soloContraLaBaseDePruebas() {
        System.setProperty("nutrimaker.db.url", URL_PRUEBAS);
        System.setProperty("nutrimaker.db.user", USUARIO_PRUEBAS);
        System.setProperty("nutrimaker.db.password", PASSWORD_PRUEBAS);

        String base = null;
        String motivo = "";
        try (Connection c = new DatabaseClient().connect()) {
            base = c.getCatalog();
        } catch (SQLException e) {
            motivo = " (sin conexión a MySQL: " + e.getMessage() + ")";
        }
        assumeTrue(BASE_DE_PRUEBAS.equals(base),
                "Las pruebas de integración solo corren contra la base " + BASE_DE_PRUEBAS
                + ". Conectado a: " + base + motivo);
    }

    @AfterAll
    static void restaurarConexion() {
        System.clearProperty("nutrimaker.db.url");
        System.clearProperty("nutrimaker.db.user");
        System.clearProperty("nutrimaker.db.password");
    }

    @AfterEach
    void limpiar() throws Exception {
        for (String correo : correosCreados) {
            db.transaction(c -> execute(c, "DELETE FROM USUARIO WHERE CORREO=?", correo));
        }
        correosCreados.clear();
    }

    // Correo único (máx. 50 caracteres) que se borra al terminar la prueba.
    private String correoNuevo(String prefijo) {
        String correo = prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@nutri.test";
        correosCreados.add(correo);
        return correo;
    }

    // Inserta un usuario directo en la tabla (sin usar UserRepository) con la contraseña en hash BCrypt.
    private void sembrarUsuario(String correo, String contrasena) throws Exception {
        String hash = BCrypt.hashpw(contrasena, BCrypt.gensalt());
        db.transaction(c -> {
            int id = nextId(c, "USUARIO", "ID_USUARIO");
            return execute(c, "INSERT INTO USUARIO (ID_USUARIO,CORREO,PASSWORD) VALUES (?,?,?)", id, correo, hash);
        });
    }

    private JsonArray filasDe(String correo) throws Exception {
        return db.read(c -> query(c, "SELECT ID_USUARIO AS id, PASSWORD AS password FROM USUARIO WHERE CORREO=?", correo));
    }

    /**
     * @objetivo Verificar que un usuario guardado en la base pueda autenticarse y que las credenciales incorrectas se rechacen.
     * @precondicion Usuario sembrado en nutrimaker_test con su contraseña en hash BCrypt.
     * @pasos 1.- Verificar la contraseña correcta del usuario; 2.- Verificar una contraseña incorrecta; 3.- Verificar un correo inexistente
     * @esperado La contraseña correcta devuelve true; la incorrecta y el correo inexistente devuelven false sin lanzar excepción
     * Comentarios: No verifica AES-128 ni TLS 1.2 (parte del RF-01); el código usa BCrypt.
     */
    @TC(id = "ITS-01", rf = "RF-01", modulo = "usuarios")
    @Test
    void ITS_01_autenticarUsuarioAlmacenadoEnMySQL() throws Exception {
        String correo = correoNuevo("its01");
        sembrarUsuario(correo, "Abc12345");

        assertTrue(users.verifyPasswordByEmail(correo, "Abc12345"), "contraseña correcta");
        assertFalse(users.verifyPasswordByEmail(correo, "Abc12346"), "contraseña incorrecta");
        assertFalse(users.verifyPasswordByEmail(correo, "abc12345"), "otra capitalización");
        assertFalse(users.verifyPasswordByEmail(correoNuevo("its01-inexistente"), "Abc12345"), "correo inexistente");

        assertNotNull(users.getIdByEmail(correo), "el usuario existe");
        assertNull(users.getIdByEmail("its01-nadie@nutri.test"), "correo inexistente no tiene id");
    }

    /**
     * @objetivo Verificar que el alta guarde el usuario en MySQL y que la contraseña no quede en texto plano.
     * @precondicion La tabla USUARIO no contiene el correo nuevo.
     * @pasos 1.- Registrar un correo nuevo con la contraseña Abc12345; 2.- Consultar la fila con SELECT; 3.- Comparar la contraseña guardada con el texto plano
     * @esperado El alta devuelve true; existe una fila con ese correo; la contraseña guardada es un hash BCrypt distinto del texto plano
     */
    @TC(id = "ITS-02", rf = "RF-02", modulo = "usuarios")
    @Test
    void ITS_02_registrarUsuarioEnMySQL() throws Exception {
        String correo = correoNuevo("its02");

        assertTrue(users.insertUser(correo, "Abc12345"), "el alta termina sin error");

        JsonArray filas = filasDe(correo);
        assertEquals(1, filas.size(), "existe una fila con ese correo");
        String guardada = filas.get(0).getAsJsonObject().get("password").getAsString();
        assertNotEquals("Abc12345", guardada, "no se guarda en texto plano");
        assertTrue(guardada.startsWith("$2"), "tiene prefijo BCrypt");
        assertEquals(60, guardada.length(), "longitud de un hash BCrypt");
        assertTrue(BCrypt.checkpw("Abc12345", guardada), "el hash corresponde a la contraseña");
        assertTrue(users.verifyPasswordByEmail(correo, "Abc12345"), "el usuario nuevo puede autenticarse");
    }

    /**
     * @objetivo Verificar que no se registren dos usuarios con el mismo correo y que el primero no se altere.
     * @precondicion Existe un usuario sembrado con ese correo.
     * @pasos 1.- Intentar registrar otra vez el mismo correo con otra contraseña; 2.- Contar las filas con ese correo; 3.- Verificar que la contraseña original sigue funcionando
     * @esperado El segundo alta devuelve false sin lanzar excepción; hay una sola fila; solo la contraseña original autentica
     * Comentarios: La restricción UNIQUE (UNIQ_CORREO) de la tabla es la que rechaza el duplicado.
     */
    @TC(id = "ITS-03", rf = "RF-02", modulo = "usuarios")
    @Test
    void ITS_03_rechazarRegistroConCorreoDuplicado() throws Exception {
        String correo = correoNuevo("its03");
        sembrarUsuario(correo, "Abc12345");

        assertFalse(users.insertUser(correo, "Otra123456"), "el duplicado se rechaza");

        assertEquals(1, filasDe(correo).size(), "sigue habiendo una sola fila");
        assertTrue(users.verifyPasswordByEmail(correo, "Abc12345"), "la contraseña original sigue válida");
        assertFalse(users.verifyPasswordByEmail(correo, "Otra123456"), "la contraseña del intento duplicado no sirve");
    }
}