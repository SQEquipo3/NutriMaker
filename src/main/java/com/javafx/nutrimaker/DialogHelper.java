package com.javafx.nutrimaker;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Único lugar donde se abren las ventanas de aviso de la aplicación.
 * Antes cada controlador tenía su propia copia de dialog() y los nombres de los FXML escritos a mano.
 */
public final class DialogHelper {

    /** Aviso con un mensaje de texto (usa el label warningLabel de DialogController). */
    public static final String AVISO = "DialogInputs.fxml";
    /** Ventana de credenciales inválidas. El nombre debe coincidir con el archivo, incluidas las mayúsculas. */
    public static final String CREDENCIALES_INVALIDAS = "checkCredentials.fxml";
    public static final String CORREO_EXISTENTE = "EmailExists.fxml";
    public static final String REGISTRO_COMPLETADO = "RegisterSuccessful.fxml";

    private DialogHelper() {}

    /** Muestra el aviso con el mensaje indicado. */
    public static void showMessage(String message) throws IOException {
        FXMLLoader loader = new FXMLLoader(requireResource(AVISO));
        Parent parent = loader.load();
        DialogController dialog = loader.getController();
        dialog.setText(message);
        show(parent);
    }

    /** Muestra una ventana cuyo contenido completo viene de un FXML (por ejemplo, CREDENCIALES_INVALIDAS). */
    public static void showFxml(String fxmlName) throws IOException {
        Parent parent = FXMLLoader.load(requireResource(fxmlName));
        show(parent);
    }

    private static void show(Parent parent) {
        Stage stage = new Stage();
        stage.setScene(new Scene(parent));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private static URL requireResource(String name) throws IOException {
        URL url = DialogHelper.class.getResource(name);
        if (url == null) {
            throw new IOException("No se encontró " + name + " en resources. Revisa que el nombre coincida exactamente.");
        }
        return url;
    }
}
