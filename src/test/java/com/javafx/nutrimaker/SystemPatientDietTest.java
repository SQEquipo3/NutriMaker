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
     * @nombre Registrar un paciente desde el formulario
     * @objetivo Verificar la captura de un paciente desde la interfaz: el formulario «Nuevo paciente»
     * (PatientFormController) muestra un aviso con dialog() cuando checkInputs() rechaza los datos, y
     * con datos válidos guarda el paciente con PatientRepository y abre la pantalla «Crear dieta»
     * (RF-05; URS_23 y URS_24).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Sesión iniciada con un usuario temporal.
     * @pasos 1.- Pulsar #createButton (Nueva) en el almacenamiento de dietas;
     * 2.- Capturar nombre, edad 34 y peso 62.5 con la altura vacía, y pulsar #saveButton;
     * 3.- Capturar la edad «treinta» y pulsar #saveButton;
     * 4.- Capturar edad 34, peso 62.5 y altura 1.65 y pulsar #saveButton.
     * @esperado Se abre el formulario; la altura vacía muestra «No pueden existir campos vacios» y la edad con
     * letras «Solo se permiten números enteros», sin guardar; con datos válidos se abre «Crear dieta»
     * y el paciente queda en PACIENTE con esos datos.
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
     * @nombre Generar una dieta desde la interfaz
     * @objetivo Verificar la generación de una dieta desde la interfaz: en «Crear dieta»,
     * CreateDietController.save() valida con checkInputs(), llama a MealRepository.createNewDiet() y
     * regresa al almacenamiento, donde aparece la dieta nueva (RF-04; URS_20 y URS_21).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Sesión iniciada y paciente capturado
     * desde el formulario «Nuevo paciente».
     * @pasos 1.- Capturar 2000 en #calTextField, 3 en #quantityFoodDistribution, «Domingo» en #selectFreeDay
     * y un comentario;
     * 2.- Pulsar #saveButton;
     * 3.- Esperar el aviso «¡La dieta se ha creado con exito!» y la pantalla «Almacenamiento de
     * Dietas»;
     * 4.- Leer la primera fila de la tabla y contar sus comidas en COMIDA.
     * @esperado Aparece el aviso de éxito; la tabla muestra la dieta con el paciente y la fecha de hoy; en
     * MySQL tiene 18 comidas y ninguna el domingo.
     * @nota El issue #61 (fecha de mañana al crear de noche) ya está cerrado.
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
     * @nombre Ver y paginar las dietas almacenadas
     * @objetivo Verificar la pantalla «Almacenamiento de Dietas» con muchas dietas: DietStorageController
     * muestra 10 dietas por página (DietRepository.getDiets) con paciente, peso, altura y fecha, y
     * los botones #prevButton y #nextButton solo aparecen cuando hay página anterior o siguiente
     * (RF-03; URS_14).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Usuario con 25 dietas creadas con
     * DietRepository.createDiet().
     * @pasos 1.- Iniciar sesión y revisar la primera página;
     * 2.- Pulsar #nextButton para ir a la página 2 y luego a la 3;
     * 3.- Pulsar #prevButton para regresar a la página 2.
     * @esperado La página 1 tiene 10 dietas con paciente, peso, altura y la fecha de hoy, sin «anterior»; las
     * páginas 2 y 3 tienen 10 y 5 dietas, sin repetir ninguna de las 25; en la última no hay
     * «siguiente»; al regresar se ve la misma página 2.
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
     * @nombre Navegar entre las pantallas de dietas
     * @objetivo Verificar la navegación entre las pantallas de gestión de dietas sin errores: almacenamiento →
     * «Nuevo paciente» → «Crear dieta» → almacenamiento, y abrir y cancelar los diálogos «Editar
     * Dieta» y Eliminar, sin cerrar la ventana principal ni modificar dietas (RF-03; URS_19: acceso a
     * las opciones principales; URS_30: cancelar el borrado).
     * @precondicion Prueba de interfaz con TestFX sobre la aplicación JavaFX real (clase base
     * NutriMakerGuiTest) y la base MySQL nutrimaker_test; en CI corre bajo Xvfb. Los usuarios,
     * pacientes y dietas que crea se borran al terminar. Sesión iniciada con una dieta generada
     * con MealRepository.createNewDiet().
     * @pasos 1.- Pulsar #createButton, capturar un paciente y guardar para llegar a «Crear dieta»;
     * 2.- Capturar 2000 kcal, 3 comidas, «Domingo» y un comentario, y guardar;
     * 3.- Pulsar Editar en una fila y luego #cancelButton;
     * 4.- Pulsar Eliminar en otra fila y luego #cancelBtn.
     * @esperado Cada pantalla se abre en la misma ventana; al guardar se regresa al almacenamiento con las dos
     * dietas; los diálogos se cierran al cancelar, el título sigue siendo «Almacenamiento de Dietas»
     * y las dietas no cambian.
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
