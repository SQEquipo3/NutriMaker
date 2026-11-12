package com.javafx.nutrimaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Test;

/*
 * Revisa que los FXML de las ventanas de aviso existan con el nombre EXACTO que usa DialogHelper.
 * Windows no distingue mayúsculas de minúsculas, pero Linux (donde corre el CI) sí; por eso se compara
 * el nombre real del archivo y no solo se comprueba que el recurso se encuentre.
 * No lleva ID de TC en el nombre, así que no cuenta en el reporte de casos de prueba.
 */
class DialogHelperTest {

    @Test
    void fxmlDeLasVentanasDeAvisoExistenConNombreExacto() throws Exception {
        String[] nombres = {
            DialogHelper.AVISO,
            DialogHelper.CREDENCIALES_INVALIDAS,
            DialogHelper.CORREO_EXISTENTE,
            DialogHelper.REGISTRO_COMPLETADO
        };
        for (String nombre : nombres) {
            URL url = DialogHelper.class.getResource(nombre);
            assertNotNull(url, "No se encuentra el recurso " + nombre);
            String enDisco = new File(url.toURI()).getCanonicalFile().getName();
            assertEquals(nombre, enDisco, "El archivo se llama " + enDisco + " y el código pide " + nombre);
        }
    }
}
