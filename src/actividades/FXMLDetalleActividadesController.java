/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package actividades;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import upv.ipc.sportlib.Activity;

/**
 * FXML Controller class
 *
 * @author editor
 */
public class FXMLDetalleActividadesController implements Initializable {

    @FXML
    private Label lblDistancia;
    @FXML
    private ScrollPane mapaDetalle;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
    public void setActividad(Activity actividadSeleccionada, Group nodoMapa) {
    
    // (Tu código de los labels aquí: lblDistancia.setText(...))

    // Comprobaciones de seguridad
    if (mapaDetalle == null) {
        System.out.println("DEBUG FATAL: contenedorMapaDetalle es NULL. ¡Falta poner el fx:id en Scene Builder o hacer 'Make Controller'!");
        return;
    }

    if (nodoMapa == null) {
        System.out.println("DEBUG: El nodoMapa llegó NULL al controlador de detalle. Ponemos un texto de aviso.");
        mapaDetalle.setContent(new Label("No hay mapa disponible para esta ruta."));
    } else {
        System.out.println("DEBUG: ¡El nodoMapa llegó perfectamente! Incrustándolo en el ScrollPane...");
        mapaDetalle.setContent(nodoMapa);
    }
}
}

