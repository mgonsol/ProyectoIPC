package actividades;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

public class FXMLActividadesController implements Initializable {

    @FXML private Label tiempoTotal;
    @FXML private Label distanciaAcumulada;
    @FXML private Label metrosAscensoTotales;
    @FXML private Label metrosDescensoTotales;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarEstadisticasAcumuladas();
    }

    private void cargarEstadisticasAcumuladas() {
        SportActivityApp app = SportActivityApp.getInstance();
        List<Activity> actividades = app.getUserActivities();

        double distanciaTotal = 0.0;
        long tiempoTotalSegundos = 0;
        double ascensoTotal = 0.0;
        double descensoTotal = 0.0;

        if (actividades != null) {
            for (Activity act : actividades) {
                distanciaTotal += act.getTotalDistance() / 1000.0;
                tiempoTotalSegundos += act.getDuration().toSeconds();
                ascensoTotal += act.getElevationGain();
                descensoTotal += act.getElevationLoss();
            }
        }

        long horas = tiempoTotalSegundos / 3600;
        long minutos = (tiempoTotalSegundos % 3600) / 60;

        distanciaAcumulada.setText(String.format("%.2f km", distanciaTotal));
        tiempoTotal.setText(String.format("%dh %02dm", horas, minutos));
        metrosAscensoTotales.setText(String.format("%.0f m", ascensoTotal));
        metrosDescensoTotales.setText(String.format("%.0f m", descensoTotal));
    }
}