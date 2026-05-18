/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    private void cancel(ActionEvent event) {
        System.exit(0);
    }
    private void checkNickname(){
        User.checkNickName(nicknameField1.getText());
    }
    
    private void checkPassword() {
        User.checkPassword(passwordField1.getText());
    }

    @FXML
    private void autenticarse1(ActionEvent event) {
        if(app.login(nicknameField1.getText(), passwordField1.getText())){
            //cambar vista a la pagina principal
        }else{
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Nickname o contraseña incorrectos");
            alert.showAndWait();
        }
    }

    @FXML
    private void cancel1(ActionEvent event) {
        System.exit(0);
    }
}
