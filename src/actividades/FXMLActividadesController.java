/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package actividades;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

/**
 * FXML Controller class
 *
 * @author editor
 */
public class FXMLActividadesController implements Initializable {

    @FXML
    private Label tiempoTotal;
    @FXML
    private Label distanciaAcumulada;
    @FXML
    private Label metrosAscensoTotales;
    @FXML
    private Label metrosDescensoTotales;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        // 1. Obtenemos la instancia de la aplicación y la lista de actividades del usuario
        SportActivityApp app = SportActivityApp.getInstance();
        List<Activity> actividades = app.getUserActivities();
        
        // Variables para ir sumando los totales
        double distanciaTotal = 0.0;
        long tiempoTotalSegundos = 0;
        double ascensoTotal = 0.0;
        double descensoTotal = 0.0;
        
        // 2. Recorremos todas las actividades y vamos acumulando
        if (actividades != null) {
            for (Activity act : actividades) {
                // Sumamos la distancia (la pasamos a km directamente)
                distanciaTotal += act.getTotalDistance() / 1000.0;
                
                // Sumamos el tiempo en segundos para luego formatearlo bien
                tiempoTotalSegundos += act.getDuration().toSeconds();
                
                // Sumamos los desniveles
                ascensoTotal += act.getElevationGain();
                descensoTotal += act.getElevationLoss();
            }
        }
        
        // 3. Formateamos el tiempo total (de segundos a Horas y Minutos)
        long horas = tiempoTotalSegundos / 3600;
        long minutos = (tiempoTotalSegundos % 3600) / 60;
        
        // 4. Actualizamos los Labels en la interfaz
        distanciaAcumulada.setText(String.format("%.2f km", distanciaTotal));
        tiempoTotal.setText(String.format("%dh %02dm", horas, minutos));
        metrosAscensoTotales.setText(String.format("%.0f m", ascensoTotal));
        metrosDescensoTotales.setText(String.format("%.0f m", descensoTotal));
    }    
    
}
