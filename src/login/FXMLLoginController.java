package login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

public class FXMLLoginController implements Initializable {

    @FXML
    private PasswordField passwordField1;
    @FXML
    private TextField nicknameField1;
    
    SportActivityApp app = SportActivityApp.getInstance();
    @FXML
    private Label passwordError;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            Stage stage = (Stage) nicknameField1.getScene().getWindow();
            stage.setMinWidth(640);
            stage.setMinHeight(460);
        });
    }

    @FXML
    private void autenticarse(ActionEvent event) {
        if (app.login(nicknameField1.getText(), passwordField1.getText())) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mapademo/FXMLDocument.fxml"));
            Pane pane = loader.load();
            
            // 1. Obtenemos la ventana física (Stage) a partir del campo de texto
            Stage ventana = (Stage) nicknameField1.getScene().getWindow();
            
            // 2. En lugar de solo cambiar el Root, creamos una escena nueva con el tamaño grande
            Scene escenaPrincipal = new Scene(pane, 1200, 800);
            ventana.setScene(escenaPrincipal);
            
            // 3. Centramos la ventana para que al crecer no se quede en una esquina del monitor
            ventana.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    } else {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Nickname o contraseña incorrectos");
        alert.showAndWait();
    }
    }

    @FXML
    private void cancel(ActionEvent event) {
        System.exit(0);
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

}
