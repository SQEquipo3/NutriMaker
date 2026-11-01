package com.javafx.nutrimaker;

import com.javafx.nutrimaker.models.ComidaProgramada;
import com.javafx.nutrimaker.models.Diet;
import com.javafx.nutrimaker.models.DistribuidorDeCalorias;
import com.javafx.nutrimaker.qa.TC;
import com.javafx.nutrimaker.repository.DietRepository;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias UTS-05 a UTS-10: validaciones de dieta, paciente, edición y exportación.
 * Llaman a los métodos reales de los controladores y modelos; los avisos que esos métodos
 * mostrarían en pantalla se capturan para revisar el mensaje sin abrir ventanas.
 */
public class DietAndPatientControllerTest {

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Si ya está iniciado, se ignora
        }
        // UTS-09 abre y cierra Alerts: sin esto JavaFX se apaga al cerrarse la última ventana.
        Platform.setImplicitExit(false);
    }

    // ---------------------------------------------------------------- utilidades

    /** Asigna un campo privado (@FXML) del controlador, buscándolo en su clase o sus superclases. */
    private static void poner(Object objeto, String campo, Object valor) throws Exception {
        for (Class<?> c = objeto.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(campo);
                f.setAccessible(true);
                f.set(objeto, valor);
                return;
            } catch (NoSuchFieldException ignorado) {
                // sigue con la superclase
            }
        }
        throw new NoSuchFieldException(campo);
    }

    private static ChoiceBox<String> opcion(String valor) {
        ChoiceBox<String> c = new ChoiceBox<>();
        if (valor != null) {
            c.getItems().add(valor);
            c.setValue(valor);
        }
        return c;
    }

    /** Invoca un método privado y regresa su resultado, desenvolviendo la excepción real. */
    private static Object invocar(Class<?> clase, Object objeto, String metodo) throws Exception {
        Method m = clase.getDeclaredMethod(metodo);
        m.setAccessible(true);
        try {
            return m.invoke(objeto);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    /** CreateDietController cuyo aviso dialog(...) solo se anota, con los campos del formulario llenos. */
    private static final class CrearDieta extends CreateDietController {
        final List<String> avisos = new ArrayList<>();
        CrearDieta(String kcal, String comidas, String descanso, String comentario) throws Exception {
            poner(this, "calTextField", new TextField(kcal));
            poner(this, "quantityFoodDistribution", opcion(comidas));
            poner(this, "selectFreeDay", opcion(descanso));
            poner(this, "commentTextField", new TextField(comentario));
        }
        @Override public void dialog(String mensaje) { avisos.add(mensaje); }
        boolean validar() throws Exception {
            return (Boolean) invocar(CreateDietController.class, this, "checkInputs");
        }
    }

    /** PatientFormController cuyo aviso dialog(...) solo se anota, con los campos del paciente llenos. */
    private static final class FormPaciente extends PatientFormController {
        final List<String> avisos = new ArrayList<>();
        FormPaciente(String nombre, String edad, String peso, String altura) throws Exception {
            poner(this, "nameTextField", new TextField(nombre));
            poner(this, "ageTextField", new TextField(edad));
            poner(this, "weightTextField", new TextField(peso));
            poner(this, "heightTextField", new TextField(altura));
        }
        @Override public void dialog(String mensaje) { avisos.add(mensaje); }
    }

    /** Valida con CreateDietController.checkInputs() y regresa el aviso mostrado (null si se aceptó). */
    private static String avisoAlCrear(String kcal) throws Exception {
        CrearDieta c = new CrearDieta(kcal, "3", "Domingo", "Comentario QA");
        boolean ok = c.validar();
        assertEquals(ok, c.avisos.isEmpty(), "Debe mostrarse un aviso si y solo si se rechaza: " + kcal);
        return ok ? null : c.avisos.get(0);
    }

    /** Valida con PatientFormController.checkInputs() y regresa el aviso mostrado (null si se aceptó). */
    private static String avisoPaciente(String nombre, String edad, String peso, String altura) throws Exception {
        FormPaciente p = new FormPaciente(nombre, edad, peso, altura);
        boolean ok = p.checkInputs();
        assertEquals(ok, p.avisos.isEmpty(), "Debe mostrarse un aviso si y solo si se rechaza");
        return ok ? null : p.avisos.get(0).trim();
    }

    /**
     * Ejecuta EditDietController.save() en el hilo de JavaFX con los datos dados y regresa el texto del
     * Alert de error que muestra (el Alert se cierra solo). Los datos son inválidos, así que save()
     * nunca llega a DietRepository.updateDietForUser() ni a la base de datos.
     */
    private static String errorAlEditar(String kcal, String comidas, String descanso, String comentario) throws Exception {
        EditDietController editor = new EditDietController();
        poner(editor, "calTextField", new TextField(kcal));
        poner(editor, "quantityFoodDistribution", opcion(comidas));
        poner(editor, "selectFreeDay", opcion(descanso));
        poner(editor, "commentTextField", new TextField(comentario));
        AtomicReference<Throwable> falla = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                invocar(EditDietController.class, editor, "save");
            } catch (Throwable t) {
                falla.set(t);
            }
        });
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            if (falla.get() != null) fail("save() lanzó " + falla.get() + " en lugar de mostrar un mensaje");
            String texto = enFx(() -> {
                for (Window w : new ArrayList<>(Window.getWindows())) {
                    Node pane = w.isShowing() && w.getScene() != null ? w.getScene().getRoot().lookup(".dialog-pane") : null;
                    if (pane instanceof DialogPane d) {
                        w.hide();
                        return d.getContentText();
                    }
                }
                return null;
            });
            if (texto != null) return texto;
            Thread.sleep(50);
        }
        return fail("save() no mostró ningún mensaje de error");
    }

    private static <T> T enFx(java.util.concurrent.Callable<T> accion) throws Exception {
        CompletableFuture<T> r = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                r.complete(accion.call());
            } catch (Throwable t) {
                r.completeExceptionally(t);
            }
        });
        return r.get(10, TimeUnit.SECONDS);
    }

    // ==========================================
    // PRUEBA UTS-05 (Validar calorías de la dieta)
    // ==========================================
    /**
     * @nombre Validación de calorías al crear una dieta
     * @objetivo Verificar que CreateDietController.checkInputs() (pantalla «Crear dieta») solo deje generar
     * una dieta con calorías enteras entre 1 y 6000 y con todos los campos capturados, y que en cada rechazo
     * muestre el aviso correspondiente con dialog(...). Es la validación que corre antes de llamar a
     * MealRepository.createNewDiet(), así que protege que no se generen dietas con parámetros imposibles
     * (URS_21: campos obligatorios antes de generar; URS_12/URS_42: mensajes de error claros).
     * @precondicion CreateDietController con calTextField, quantityFoodDistribution («3»), selectFreeDay
     * («Domingo») y commentTextField asignados por reflexión; dialog(...) sobrescrito para anotar el mensaje
     * en lugar de abrir DialogInputs.fxml. No usa base de datos.
     * @pasos 1.- Llamar checkInputs() con el comentario vacío;
     * 2.- Llamarlo con calorías «abc», «-1» y «20.5»;
     * 3.- Llamarlo con calorías «0» y «6001»;
     * 4.- Llamarlo con calorías «1» y «6000»;
     * 5.- Llamarlo con calorías «99999999999», un número que no cabe en un int.
     * @esperado Comentario vacío: false y aviso «No pueden existir entradas vacías»; «abc», «-1» y «20.5»:
     * false y «Solo se permiten números»; «0» y «6001»: false y «Rango de calorías permitido 0 - 6000»;
     * «1» y «6000»: true sin aviso; «99999999999»: false con el aviso de rango, sin lanzar excepción.
     * @nota Hoy el último caso falla: checkInputs() hace Integer.parseInt() y lanza NumberFormatException
     * en vez de mostrar el aviso (issue #66).
     */
    @TC(id="UTS-05", rf="RF-04", modulo="dietas")
    @Test
    public void UTS_05_validarCaloriasDietas() throws Exception {
        CrearDieta sinComentario = new CrearDieta("2000", "3", "Domingo", "");
        assertFalse(sinComentario.validar());
        assertEquals(List.of("No pueden existir entradas vacías"), sinComentario.avisos);

        for (String kcal : new String[]{"abc", "-1", "20.5"}) {
            assertEquals("Solo se permiten números", avisoAlCrear(kcal), kcal);
        }
        for (String kcal : new String[]{"0", "6001"}) {
            assertEquals("Rango de calorías permitido 0 - 6000", avisoAlCrear(kcal), kcal);
        }
        assertNull(avisoAlCrear("1"));
        assertNull(avisoAlCrear("6000"));

        String aviso = assertDoesNotThrow(() -> avisoAlCrear("99999999999"),
                "Un número enorme debe mostrar el aviso de rango, no lanzar una excepción");
        assertEquals("Rango de calorías permitido 0 - 6000", aviso);
    }

    // ==========================================
    // PRUEBA UTS-06 (Reparto de calorías por comida)
    // ==========================================
    /**
     * @nombre Reparto de calorías por comida
     * @objetivo Verificar el reparto de calorías que hace DistribuidorDeCalorias.distribuir(caloriasTotales,
     * comidasPorDia) (models/DistribuidorDeCalorias.java), que MealRepository.createNewDiet() usa para
     * decidir cuántas calorías lleva cada comida del día: para cada cantidad de comidas que ofrece la
     * pantalla «Crear dieta» debe dar una comida por tiempo, con calorías positivas que sumen el total, y
     * debe rechazar cantidades sin plan definido. También revisa obtenerTiempos() y obtenerHoras().
     * @precondicion CreateDietController.initialize() carga las opciones de quantityFoodDistribution
     * (saveButton asignado por reflexión para su animación). No usa base de datos.
     * @pasos 1.- Leer las opciones de quantityFoodDistribution después de initialize();
     * 2.- Para cada opción n, llamar distribuir(2000, n) y sumar getCaloriasAsignadas();
     * 3.- Llamar obtenerTiempos(n) y obtenerHoras(n);
     * 4.- Llamar distribuir(2000, 0) y distribuir(2000, 7).
     * @esperado Las opciones son 3, 4, 5 y 6; cada reparto tiene n comidas con calorías > 0 que suman 2000
     * (tolerancia 0.01 kcal); hay n tiempos y n horas en orden ascendente; 0 y 7 lanzan
     * IllegalArgumentException «El número de comidas debe estar entre 1 y 6.».
     */
    @TC(id="UTS-06", rf="RF-04", modulo="dietas")
    @Test
    public void UTS_06_parametrosGeneracionDietas() throws Exception {
        CreateDietController pantalla = new CreateDietController();
        poner(pantalla, "saveButton", new Button());
        poner(pantalla, "quantityFoodDistribution", new ChoiceBox<String>());
        poner(pantalla, "selectFreeDay", new ChoiceBox<String>());
        enFx(() -> { pantalla.initialize(null, null); return null; });
        Field campo = CreateDietController.class.getDeclaredField("quantityFoodDistribution");
        campo.setAccessible(true);
        List<String> opciones = ((ChoiceBox<?>) campo.get(pantalla)).getItems().stream().map(Objects::toString).toList();
        assertEquals(List.of("3", "4", "5", "6"), opciones);

        for (String opcion : opciones) {
            int n = Integer.parseInt(opcion);
            List<ComidaProgramada> reparto = DistribuidorDeCalorias.distribuir(2000, n);
            assertEquals(n, reparto.size(), "comidas para " + n);
            assertTrue(reparto.stream().allMatch(c -> c.getCaloriasAsignadas() > 0), "calorías positivas para " + n);
            assertEquals(2000, reparto.stream().mapToDouble(ComidaProgramada::getCaloriasAsignadas).sum(), 0.01,
                    "el reparto de " + n + " comidas debe sumar el total");
            assertEquals(n, DistribuidorDeCalorias.obtenerTiempos(n).length);
            List<LocalTime> horas = DistribuidorDeCalorias.obtenerHoras(n);
            assertEquals(n, horas.size());
            for (int i = 1; i < horas.size(); i++) assertTrue(horas.get(i - 1).isBefore(horas.get(i)), "horas en orden");
        }
        for (int invalido : new int[]{0, 7}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> DistribuidorDeCalorias.distribuir(2000, invalido));
            assertEquals("El número de comidas debe estar entre 1 y 6.", e.getMessage());
        }
    }

    // ==========================================
    // PRUEBA UTS-07 (Nombre y edad del paciente)
    // ==========================================
    /**
     * @nombre Nombre y edad del paciente
     * @objetivo Verificar que PatientFormController.checkInputs() (formulario «Nuevo paciente») exija el
     * nombre y la edad, y que la edad sea un número entero, mostrando el aviso que corresponde con
     * dialog(...). Es la validación previa a PatientRepository.createPatientAndGetId(), así que evita
     * pacientes sin nombre o con edad no numérica (URS_23: campos obligatorios; URS_24: formato).
     * @precondicion PatientFormController con nameTextField, ageTextField, weightTextField («64.5») y
     * heightTextField («1.68») asignados por reflexión; dialog(...) sobrescrito para anotar el mensaje.
     * No usa base de datos.
     * @pasos 1.- Llamar checkInputs() con el nombre vacío;
     * 2.- Llamarlo con la edad vacía;
     * 3.- Llamarlo con edad «abc», «-5» y «29.5»;
     * 4.- Llamarlo con nombre «Ana» y edad «30».
     * @esperado Nombre o edad vacíos: false y aviso «No pueden existir campos vacios»; «abc», «-5» y «29.5»:
     * false y «Solo se permiten números enteros»; nombre y edad válidos: true sin aviso.
     * @nota La especificación no define un rango de edad; checkInputs() acepta cualquier entero no negativo.
     */
    @TC(id="UTS-07", rf="RF-05", modulo="pacientes")
    @Test
    public void UTS_07_validarEdadYNombrePaciente() throws Exception {
        assertEquals("No pueden existir campos vacios", avisoPaciente("", "30", "64.5", "1.68"));
        assertEquals("No pueden existir campos vacios", avisoPaciente("Ana", "", "64.5", "1.68"));
        for (String edad : new String[]{"abc", "-5", "29.5"}) {
            assertEquals("Solo se permiten números enteros", avisoPaciente("Ana", edad, "64.5", "1.68"), edad);
        }
        assertNull(avisoPaciente("Ana", "30", "64.5", "1.68"));
    }

    // ==========================================
    // PRUEBA UTS-08 (Peso y altura del paciente)
    // ==========================================
    /**
     * @nombre Peso y altura del paciente
     * @objetivo Verificar que PatientFormController.checkInputs() exija el peso y la altura y que ambos
     * sean números positivos, enteros o con decimales (expresión REG_EXP_POSIT_FLOAT), mostrando el aviso
     * que corresponde. Con estos datos se calculan después los requerimientos de la dieta, por eso no
     * deben aceptarse letras ni negativos (URS_23: campos obligatorios; URS_24: formato).
     * @precondicion PatientFormController con nombre «Ana» y edad «30», y los campos de peso y altura
     * asignados por reflexión; dialog(...) sobrescrito para anotar el mensaje. No usa base de datos.
     * @pasos 1.- Llamar checkInputs() con el peso vacío y luego con la altura vacía;
     * 2.- Llamarlo con peso «abc» y con altura «-1.5»;
     * 3.- Llamarlo con peso «70.5» y altura «1.75»;
     * 4.- Llamarlo con peso «70» y altura «2».
     * @esperado Peso o altura vacíos: false y «No pueden existir campos vacios»; letras o negativos: false
     * y «Solo se permiten números»; los decimales y los enteros positivos: true sin aviso.
     * @nota La especificación no define rangos de peso ni de altura; checkInputs() solo revisa el formato.
     */
    @TC(id="UTS-08", rf="RF-05", modulo="pacientes")
    @Test
    public void UTS_08_validarAlturaYPesoPaciente() throws Exception {
        assertEquals("No pueden existir campos vacios", avisoPaciente("Ana", "30", "", "1.75"));
        assertEquals("No pueden existir campos vacios", avisoPaciente("Ana", "30", "70.5", ""));
        assertEquals("Solo se permiten números", avisoPaciente("Ana", "30", "abc", "1.75"));
        assertEquals("Solo se permiten números", avisoPaciente("Ana", "30", "70.5", "-1.5"));
        assertNull(avisoPaciente("Ana", "30", "70.5", "1.75"));
        assertNull(avisoPaciente("Ana", "30", "70", "2"));
    }

    // ==========================================
    // PRUEBA UTS-09 (Validar la edición de una dieta)
    // ==========================================
    /**
     * @nombre Validación de la edición de una dieta
     * @objetivo Verificar que la edición de una dieta rechace cambios inválidos antes de guardarlos
     * (URS_26: validar antes de aplicar cambios; URS_27: mensajes de error). EditDietController.save()
     * (diálogo «Editar dieta») debe mostrar un Alert de error y no llamar al repositorio cuando las calorías
     * no son un número entre 0 y 6000, falta la cantidad de comidas o el día de descanso, o el comentario
     * está vacío; y DietRepository.updateDietForUser() debe rechazar una edición vacía o con campos que no
     * se pueden editar (por ejemplo cambiar el paciente) antes de abrir la conexión.
     * @precondicion EditDietController con calTextField, quantityFoodDistribution, selectFreeDay y
     * commentTextField asignados por reflexión; save() se ejecuta en el hilo de JavaFX y el Alert se lee y
     * se cierra desde la prueba. Ningún caso llega a la base de datos.
     * @pasos 1.- Llamar save() con calorías «abc»;
     * 2.- Llamar save() con calorías «0» y luego «6001»;
     * 3.- Llamar save() con 2000 kcal sin elegir la cantidad de comidas;
     * 4.- Llamar save() con 2000 kcal, 3 comidas, «Domingo» y el comentario vacío;
     * 5.- Llamar DietRepository.updateDietForUser(1, 1, mapa vacío) y con {"patient_id": 5}.
     * @esperado Los Alert dicen, en orden: «Las calorías deben ser un número válido.», «Las calorías deben
     * estar entre 0 y 6000.» (dos veces), «Selecciona la cantidad de comidas y el día de descanso.» y
     * «Escribe un comentario.»; updateDietForUser lanza IllegalArgumentException en los dos casos.
     */
    @TC(id="UTS-09", rf="RF-06", modulo="dietas")
    @Test
    public void UTS_09_validarParametrosModificacionDietas() throws Exception {
        assertEquals("Las calorías deben ser un número válido.", errorAlEditar("abc", "3", "Domingo", "Nota"));
        assertEquals("Las calorías deben estar entre 0 y 6000.", errorAlEditar("0", "3", "Domingo", "Nota"));
        assertEquals("Las calorías deben estar entre 0 y 6000.", errorAlEditar("6001", "3", "Domingo", "Nota"));
        assertEquals("Selecciona la cantidad de comidas y el día de descanso.", errorAlEditar("2000", null, "Domingo", "Nota"));
        assertEquals("Escribe un comentario.", errorAlEditar("2000", "3", "Domingo", ""));

        DietRepository repositorio = new DietRepository();
        assertThrows(IllegalArgumentException.class, () -> repositorio.updateDietForUser(1, 1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> repositorio.updateDietForUser(1, 1, Map.of("patient_id", 5)));
    }

    // ==========================================
    // PRUEBA UTS-10 (Exportar a PDF con datos incompletos)
    // ==========================================
    /**
     * @nombre Exportar a PDF con datos incompletos
     * @objetivo Verificar que PDFBuilder.exportPdf(diet) maneje con un error controlado una dieta que no se
     * puede exportar (nula, o sin paciente ni comidas) y que no deje un diet.pdf a medias en la carpeta
     * Documents (URS_33: manejo de errores si falla la generación del PDF). Desde la pantalla de dietas,
     * DietStorageController llama exportPdf() con lo que regresa DietRepository.getDietObjectById(), que
     * puede ser null si la consulta falla.
     * @precondicion Carpeta ~/Documents disponible; si ya existe diet.pdf se respalda antes y se restaura al
     * final. No usa base de datos.
     * @pasos 1.- Llamar exportPdf(null);
     * 2.- Revisar si quedó el archivo ~/Documents/diet.pdf;
     * 3.- Llamar exportPdf(new Diet()), una dieta sin paciente ni comidas;
     * 4.- Revisar otra vez el archivo.
     * @esperado En ambos casos exportPdf lanza una excepción controlada (no NullPointerException) y no queda
     * ningún diet.pdf. La exportación de una dieta completa se prueba en ITS-10 y ATS-10.
     * @nota Hoy falla: generatePdf() abre el PdfWriter antes de revisar la dieta, lanza NullPointerException
     * y deja un diet.pdf dañado (issue #67).
     */
    @TC(id="UTS-10", rf="RF-08", modulo="exportacion")
    @Test
    public void UTS_10_validarDatosExportarDieta() throws Exception {
        Field ruta = PDFBuilder.class.getDeclaredField("PATH");
        ruta.setAccessible(true);
        Path pdf = Path.of((String) ruta.get(null));
        Files.createDirectories(pdf.getParent());
        Path respaldo = Files.exists(pdf)
                ? Files.move(pdf, Files.createTempFile("diet-backup", ".pdf"), StandardCopyOption.REPLACE_EXISTING) : null;
        try {
            for (Diet incompleta : new Diet[]{null, new Diet()}) {
                String caso = incompleta == null ? "dieta nula" : "dieta sin paciente ni comidas";
                Exception e = assertThrows(Exception.class, () -> PDFBuilder.exportPdf(incompleta), caso);
                assertFalse(e instanceof NullPointerException,
                        caso + ": debe ser un error controlado, no " + e.getClass().getSimpleName());
                assertFalse(Files.exists(pdf), caso + ": no debe quedar un diet.pdf a medias");
                Files.deleteIfExists(pdf);
            }
        } finally {
            Files.deleteIfExists(pdf);
            if (respaldo != null) Files.move(respaldo, pdf);
        }
    }
}
