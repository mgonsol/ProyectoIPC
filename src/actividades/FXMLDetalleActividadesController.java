package actividades;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.TrackPoint;

/**
 * FXML Controller class - SOLUCIÓN DEFINITIVA ADAPTADA A CATEGORYAXIS
 */
public class FXMLDetalleActividadesController implements Initializable {

    @FXML private Label lblDistacia; 
    @FXML private Label lblTiempo;
    @FXML private Label lblVelocidad;
    @FXML private Label lblCalorias;
    @FXML private Label lblDesnivel;
    
    @FXML private ScrollPane mapaDetalle;
    
    // Cambiamos el tipo a <String, Number> porque el eje X de tu interfaz es de texto
    @FXML private LineChart<String, Number> graficaDesnivel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    
    
    public void setActividad(Activity actividadSeleccionada, Group nodoMapa) {
        if (actividadSeleccionada == null) return;

        // 1. Calculamos y rellenamos los 5 datos de la derecha
        double distKm = actividadSeleccionada.getTotalDistance() / 1000.0;
        if (lblDistacia != null) lblDistacia.setText(String.format("%.2f km", distKm));

        long totalSegundos = actividadSeleccionada.getDuration().toSeconds();
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        if (lblTiempo != null) lblTiempo.setText(String.format("%dh %02dm", horas, minutos));

        if (totalSegundos > 0) {
            double velocidadMedia = (distKm / (totalSegundos / 3600.0));
            if (lblVelocidad != null) lblVelocidad.setText(String.format("%.1f km/h", velocidadMedia));
        } else {
            if (lblVelocidad != null) lblVelocidad.setText("0.0 km/h");
        }

        double caloriasEstimadas = distKm * 65.0;
        if (lblCalorias != null) lblCalorias.setText(String.format("%.0f kcal", caloriasEstimadas));

        if (lblDesnivel != null) lblDesnivel.setText(String.format("%.0f m", actividadSeleccionada.getElevationGain()));

        // 2. Insertamos el mapa y gestionamos el centrado automático
        if (mapaDetalle != null && nodoMapa != null) {
            mapaDetalle.setContent(nodoMapa);
            mapaDetalle.setFitToWidth(false);
            mapaDetalle.setFitToHeight(false);
            mapaDetalle.setPannable(true); 

            List<TrackPoint> puntos = actividadSeleccionada.getTrackPoints();
            if (puntos != null && !puntos.isEmpty()) {
                Platform.runLater(() -> {
                    mapaDetalle.setHvalue(0.5);
                    mapaDetalle.setVvalue(0.5);
                    
                    double contenedorAncho = nodoMapa.getBoundsInLocal().getWidth();
                    double contenedorAlto = nodoMapa.getBoundsInLocal().getHeight();
                    
                    upv.ipc.sportlib.MapRegion region = upv.ipc.sportlib.SportActivityApp.getInstance().findMapForActivity(actividadSeleccionada);
                    if (region != null && contenedorAncho > 0 && contenedorAlto > 0) {
                        upv.ipc.sportlib.MapProjection proj = new upv.ipc.sportlib.MapProjection(region, contenedorAncho, contenedorAlto);
                        javafx.geometry.Point2D pixel = proj.project(puntos.get(0));
                        
                        mapaDetalle.setHvalue(pixel.getX() / contenedorAncho);
                        mapaDetalle.setVvalue(pixel.getY() / contenedorAlto);
                    }
                });
            }
        }

        // 3. Rellenamos la gráfica transformando la distancia a String (Texto)
        if (graficaDesnivel != null) {
            graficaDesnivel.getData().clear();
            graficaDesnivel.setLegendVisible(false);
            
            List<TrackPoint> puntos = actividadSeleccionada.getTrackPoints();
            if (puntos != null && !puntos.isEmpty()) {
                XYChart.Series<String, Number> serie = new XYChart.Series<>();
                double distanciaAcumulada = 0.0;
                
                // Metemos el primer punto convirtiendo la distancia a texto
                serie.getData().add(new XYChart.Data<>("0.0", puntos.get(0).getElevation()));
                
                // Para no saturar el CategoryAxis, metemos un punto cada 10 registros
                for (int i = 0; i < puntos.size() - 1; i++) {
                    TrackPoint pActual = puntos.get(i);
                    TrackPoint pSiguiente = puntos.get(i + 1);
                    distanciaAcumulada += pActual.distanceTo(pSiguiente);
                    
                    if (i % 10 == 0 || i == puntos.size() - 2) {
                        String kmTexto = String.format("%.1f", distanciaAcumulada / 1000.0);
                        serie.getData().add(new XYChart.Data<>(kmTexto, pSiguiente.getElevation()));
                    }
                }
                
                graficaDesnivel.getData().add(serie);
            }
        }
    }
}