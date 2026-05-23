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

public class FXMLSesionesController implements Initializable {

    @FXML
    private ListView<Session> listaSesiones;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listaSesiones.setCellFactory(lv -> new ListCell<Session>() {
            @Override
            protected void updateItem(Session sesion, boolean empty) {
                super.updateItem(sesion, empty);
                if (empty || sesion == null) {
                    setText(null);
                } else {
                    long minutos = sesion.getDuration().toMinutes();
                    setText(String.format("💻 %s  |  ⏱ %d min  |  📥 %d imp  |  👁 %d vistas  |  📌 %d anot.",
                            sesion.getStartTime().toLocalDate(),
                            minutos,
                            sesion.getImportedActivities(),
                            sesion.getViewedActivities(),
                            sesion.getAnnotationsCreated()));
                }
            }
        });
        cargarHistorialSesiones();
    }

    private void cargarHistorialSesiones() {
        SportActivityApp app = SportActivityApp.getInstance();
        User usuarioActual = app.getCurrentUser();
        if (usuarioActual != null) {
            List<Session> historial = app.getSessionsByUser(usuarioActual);
            if (historial != null) {
                listaSesiones.getItems().clear();
                listaSesiones.getItems().addAll(historial);
            }
        }
    }
}