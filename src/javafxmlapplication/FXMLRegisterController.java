/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package javafxmlapplication;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;

/**
 * FXML Controller class
 *
 * @author lormac
 */
public class FXMLRegisterController implements Initializable {

    @FXML
    private TextField nicknameField;
    @FXML
    private Label nicknameError;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailError;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordError;
    @FXML
    private PasswordField password2Field;
    @FXML
    private Label passwordConfirmError;
    @FXML
    private DatePicker dateField;
    @FXML
    private Label dateError;

    private String rutaAvatarRegistro = "";
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void auntenticarse(ActionEvent event) {
    }

    

    @FXML
    private void register(ActionEvent event) {
        // Llamamos directamente a la librería con su ruta completa para evitar fallos de variables
        boolean ok = upv.ipc.sportlib.SportActivityApp.getInstance().registerUser(
                    nicknameField.getText(), emailField.getText(), 
                    passwordField.getText(), dateField.getValue(), rutaAvatarRegistro);
        if (ok) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registro completado");
            alert.setHeaderText("¡Usuario creado correctamente!");
            alert.setContentText("Ya puedes iniciar sesión con tu cuenta.");
            alert.showAndWait();
            try {
                javafx.scene.layout.Pane pane = FXMLLoader.load(getClass().getResource("/login/FXMLLogin.fxml"));
                nicknameField.getScene().setRoot(pane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo registrar el usuario");
            alert.showAndWait();
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
    }
    
}
