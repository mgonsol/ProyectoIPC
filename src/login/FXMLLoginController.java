/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author editor
 */
public class FXMLLoginController implements Initializable {

    @FXML
    private PasswordField passwordField1;
    @FXML
    private TextField nicknameField1;
    
    SportActivityApp app = SportActivityApp.getInstance();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO.
        
    } 
    
    private void checkNickname(){
        User.checkNickName(nicknameField1.getText());
    }
    
    private void checkPassword() {
        User.checkPassword(passwordField1.getText());
    }

    @FXML
    private void autenticarse1(ActionEvent event) {
        if (app.login(nicknameField1.getText(), passwordField1.getText())) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/mapademo/FXMLDocument.fxml"));
                Pane pane = loader.load();
                nicknameField1.getScene().setRoot(pane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Nickname o contraseña incorrectos");
            alert.showAndWait();
        }
    }

    @FXML
    private void irARegistro(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register/FXMLRegister.fxml"));
            Pane pane = loader.load();
            nicknameField1.getScene().setRoot(pane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel1(ActionEvent event) {
        System.exit(0);
    }
}
