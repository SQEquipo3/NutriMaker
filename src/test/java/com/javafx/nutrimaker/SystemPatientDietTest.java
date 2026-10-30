package com.javafx.nutrimaker;

import com.javafx.nutrimaker.models.DietSummary;
import com.javafx.nutrimaker.qa.TC;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de sistema STS-04, STS-05, STS-06 y STS-10: paciente, generación y listado de dietas desde JavaFX. */
public class SystemPatientDietTest extends NutriMakerGuiTest {

    private boolean visible(String selector) {
        return robot.lookup(selector).tryQuery().map(n -> n.isVisible() && n.getScene() != null).orElse(false);
    }

    /** Captura los datos del paciente en el formulario abierto. */
    private void capturarPaciente(String nombre, String edad, String peso, String altura) {
        escribir("#nameTextField", nombre);
        escribir("#ageTextField", edad);
        escribir("#weightTextField", peso);
        escribir("#heightTextField", altura);
    }

    // ==========================================
    // PRUEBA STS-04 (Registrar un paciente desde el formulario)
    // ==========================================
    /**
     * @objetivo Verificar la captura de los datos del paciente desde la interfaz.
     * @precondicion Sesión iniciada con un usuario de nutrimaker_test.
     * @pasos 1.- Pulsar Nueva en el almacenamiento; 2.- Pulsar Guardar con la altura vacía;
     * 3.- Capturar letras en la edad y guardar; 4.- Capturar nombre, edad, peso y altura válidos y guardar.
     * @esperado Se abre el formulario de paciente; los datos incompletos o con letras muestran su aviso y no se
     * guardan; con datos válidos se abre la creación de dieta y el paciente queda registrado en MySQL.
     */
    @TC(id = "STS-04", rf = "RF-05", modulo = "pacientes")
    @Test
    void STS_04_registrarPacienteDesdeFormulario() throws Exception {
        iniciarSesion(crearUsuario(), PASSWORD);
        pulsar("#createButton");
        esperar("el formulario de paciente", () -> visible("#nameTextField"));

        String nombre = prefijo + "Ana López";
        capturarPaciente(nombre, "34", "62.5", "");
        pulsar("#saveButton");
        esperarAviso("No pueden existir campos vacios");
        cerrarAviso();

        capturarPaciente(nombre, "treinta", "62.5", "1.65");
        pulsar("#saveButton");
        esperarAviso("Solo se permiten números enteros");
        cerrarAviso();
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM PACIENTE WHERE NOMBRE=?", nombre),
                "Un paciente con datos inválidos no debe guardarse");

        capturarPaciente(nombre, "34", "62.5", "1.65");
        pulsar("#saveButton");
        esperar("la pantalla de creación de dieta", () -> visible("#calTextField"));
        assertEquals(1, numero("SELECT COUNT(*) AS n FROM PACIENTE WHERE NOMBRE=? AND EDAD=34 AND PESO=62.5 AND ALTURA=1.65", nombre),
                "El paciente no quedó registrado con los datos capturados");
    }

    // ==========================================
    // PRUEBA STS-05 (Generar una dieta desde JavaFX)
    // ==========================================
    /**
     * @objetivo Verificar la generación de una dieta desde la pantalla de creación.
     * @precondicion Sesión iniciada y un paciente capturado en el formulario.
     * @pasos 1.- Capturar 2000 kcal, 3 comidas, descanso el domingo y un comentario;
     * 2.- Pulsar Guardar; 3.- Esperar el mensaje de resultado; 4.- Revisar el almacenamiento de dietas.
     * @esperado Aparece el aviso de dieta creada con éxito; el almacenamiento muestra la dieta nueva con el
     * paciente y la fecha de hoy; en MySQL tiene 18 comidas y ninguna el domingo.
     */
    @TC(id = "STS-05", rf = "RF-04", modulo = "crear-dieta")
    @Test
    void STS_05_generarDietaDesdeJavaFX() throws Exception {
        String email = crearUsuario();
        iniciarSesion(email, PASSWORD);
        pulsar("#createButton");
        esperar("el formulario de paciente", () -> visible("#nameTextField"));
        String paciente = prefijo + "Luis Pérez";
        capturarPaciente(paciente, "41", "80", "1.75");
        pulsar("#saveButton");
        esperar("la pantalla de creación de dieta", () -> visible("#calTextField"));

        escribir("#calTextField", "2000");
        elegir("#quantityFoodDistribution", "3");
        elegir("#selectFreeDay", "Domingo");
        escribir("#commentTextField", "Dieta QA STS-05");
        pulsar("#saveButton");

        esperarAviso("¡La dieta se ha creado con exito!");
        esperarPantalla("Almacenamiento de Dietas");
        esperar("la dieta en la tabla", () -> !filas().isEmpty());
        DietSummary dieta = filas().get(0);
        assertEquals(paciente, dieta.getPatientName());
        assertEquals(LocalDate.now().toString(), dieta.getCreationDate());
        assertEquals(18, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=?", dieta.getDietId()));
        assertEquals(0, numero("SELECT COUNT(*) AS n FROM COMIDA WHERE ID_DIETA=? AND DIA_SEMANA=7", dieta.getDietId()));
        cerrarAviso();
    }

    // ==========================================
    // PRUEBA STS-06 (Visualizar las dietas almacenadas)
    // ==========================================
    /**
     * @objetivo Verificar el listado de dietas y la paginación.
     * @precondicion Usuario con 25 dietas en nutrimaker_test.
     * @pasos 1.- Iniciar sesión; 2.- Revisar la primera página; 3.- Pasar a las páginas 2 y 3; 4.- Regresar a la página 2.
     * @esperado Se abre el almacenamiento con 10 dietas con paciente, peso, altura y fecha; las páginas 2 y 3
     * muestran 10 y 5 dietas sin repetirse; los botones de anterior y siguiente se muestran solo cuando aplican.
     */
    @TC(id = "STS-06", rf = "RF-03", modulo = "dietas-almacenadas")
    @Test
    void STS_06_visualizarYPaginarDietas() throws Exception {
        String email = crearUsuario();
        crearDietas(email, crearPaciente("Paginación"), 25);

        iniciarSesion(email, PASSWORD);
        esperar("la primera página", () -> filas().size() == 10);
        for (DietSummary d : filas()) {
            assertTrue(d.getPatientName().startsWith(prefijo), "Fila sin paciente");
            assertTrue(d.getWeight() > 0 && d.getHeight() > 0, "Fila sin peso o altura");
            assertEquals(LocalDate.now().toString(), d.getCreationDate());
        }
        assertFalse(visible("#prevButton"), "En la primera página no debe mostrarse «anterior»");
        assertTrue(visible("#nextButton"));

        Set<Integer> vistas = new HashSet<>();
        filas().forEach(d -> vistas.add(d.getDietId()));
        List<Integer> pagina2 = new ArrayList<>();
        pulsar("#nextButton");
        esperar("la página 2", () -> filas().size() == 10 && filas().get(0).getNumDiet() == 11);
        filas().forEach(d -> pagina2.add(d.getDietId()));
        assertTrue(visible("#prevButton") && visible("#nextButton"));

        pulsar("#nextButton");
        esperar("la página 3", () -> filas().size() == 5);
        filas().forEach(d -> vistas.add(d.getDietId()));
        vistas.addAll(pagina2);
        assertEquals(25, vistas.size(), "Las páginas repiten dietas");
        assertFalse(visible("#nextButton"), "En la última página no debe mostrarse «siguiente»");

        pulsar("#prevButton");
        esperar("volver a la página 2", () -> filas().size() == 10);
        assertEquals(pagina2, filas().stream().map(DietSummary::getDietId).toList());
    }

    // ==========================================
    // PRUEBA STS-10 (Navegación entre pantallas de gestión de dietas)
    // ==========================================
    /**
     * @objetivo Verificar que se pueda navegar entre las pantallas de gestión de dietas sin errores ni cierres.
     * @precondicion Sesión iniciada con una dieta generada.
     * @pasos 1.- Ir de almacenamiento a formulario de paciente y a creación de dieta; 2.- Guardar la dieta para
     * regresar al almacenamiento; 3.- Abrir Editar dieta y cancelar; 4.- Abrir Eliminar y cancelar.
     * @esperado Cada pantalla se abre en la misma ventana; al guardar se regresa al almacenamiento; los diálogos
     * de edición y eliminación se cierran y regresan al almacenamiento con las dietas intactas.
     */
    @TC(id = "STS-10", rf = "RF-03", modulo = "dietas-almacenadas")
    @Test
    void STS_10_navegarEntrePantallas() throws Exception {
        String email = crearUsuario();
        generarDieta(email, crearPaciente("Navegación"), "QA-UI STS-10");
        iniciarSesion(email, PASSWORD);
        esperar("la dieta existente", () -> filas().size() == 1);

        pulsar("#createButton");
        esperar("el formulario de paciente", () -> visible("#nameTextField"));
        capturarPaciente(prefijo + "Navegación 2", "25", "55", "1.60");
        pulsar("#saveButton");
        esperar("la creación de dieta", () -> visible("#calTextField"));
        escribir("#calTextField", "2000");
        elegir("#quantityFoodDistribution", "3");
        elegir("#selectFreeDay", "Domingo");
        escribir("#commentTextField", "QA-UI STS-10 nueva");
        pulsar("#saveButton");
        esperarPantalla("Almacenamiento de Dietas");
        cerrarAviso();
        esperar("las dos dietas", () -> filas().size() == 2);
        List<Integer> antes = filas().stream().map(DietSummary::getDietId).toList();

        pulsar(accion(0, "edit"));
        esperar("el diálogo Editar dieta", () -> ventana("Editar Dieta") != null);
        pulsar(robot.from(ventana("Editar Dieta").getScene().getRoot()).lookup("#cancelButton").queryButton());
        esperar("cerrar Editar dieta", () -> ventana("Editar Dieta") == null);

        pulsar(accion(1, "delete"));
        Button cancelar = robot.lookup("#cancelBtn").queryButton();
        pulsar(cancelar);
        esperar("cerrar la confirmación", () -> !cancelar.getScene().getWindow().isShowing());

        assertEquals("Almacenamiento de Dietas", titulo());
        assertEquals(antes, filas().stream().map(DietSummary::getDietId).toList(), "Las dietas cambiaron al navegar");
        assertTrue(stage.isShowing(), "La ventana principal se cerró");
    }

    private Stage ventana(String titulo) {
        return Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.isShowing() && titulo.equals(s.getTitle()))
                .map(w -> (Stage) w).findFirst().orElse(null);
    }
}
