package sesiones;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class - HISTORIAL DE SESIONES COMPLETADO
 */
public class FXMLSesionesController implements Initializable {

    // Lista visual que tus compañeros arrastrarán en el Scene Builder
    @FXML 
    private ListView<Session> listaSesiones;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuramos cómo se debe ver cada sesión en la lista de forma bonita
        listaSesiones.setCellFactory(lv -> new ListCell<Session>() {
            @Override
            protected void updateItem(Session sesion, boolean empty) {
                super.updateItem(sesion, empty);
                if (empty || sesion == null) {
                    setText(null);
                } else {
                    // Formateamos el texto: fecha de inicio y la duración en minutos
                    long minutos = sesion.getDuration().toMinutes();
                    setText(String.format("💻 Conexión: %s | ⏱ Duración: %d min", 
                            sesion.getStartTime().toString(), minutos));
                }
            }
        });

        // Cargamos los datos reales del usuario
        cargarHistorialSesiones();
    }    
    
    private void cargarHistorialSesiones() {
        SportActivityApp app = SportActivityApp.getInstance();
        User usuarioActual = app.getCurrentUser();
        
        if (usuarioActual != null) {
            // Obtenemos todas las sesiones registradas de este usuario concreto
            List<Session> historial = app.getSessionsByUser(usuarioActual);
            if (historial != null) {
                listaSesiones.getItems().clear();
                listaSesiones.getItems().addAll(historial);
            }
        }
    }
}