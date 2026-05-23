package register;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.SportActivityApp;

/**
 * FXML Controller class - REGISTRO REAL REPARADO
 */
public class FXMLRegisterController implements Initializable {

    @FXML private TextField nicknameField;
    @FXML private Label nicknameError;
    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordError;
    @FXML private PasswordField password2Field;
    @FXML private Label passwordConfirmError;
    @FXML private DatePicker dateField;
    @FXML private Label dateError;
    
    // Aquí está el visor de la foto, puesto una sola vez
    @FXML private ImageView avatarPreview; 

    // Aquí está la variable de la ruta, puesta una sola vez
    private String rutaAvatarRegistro = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    

    @FXML
    private void register(ActionEvent event) {
        boolean ok = SportActivityApp.getInstance().registerUser(
                    nicknameField.getText(), emailField.getText(), 
                    passwordField.getText(), dateField.getValue(), rutaAvatarRegistro);
        
        if (ok) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registro completado");
            alert.setHeaderText("¡Usuario creado correctamente!");
            alert.setContentText("Ya puedes iniciar sesión con tu cuenta.");
            alert.showAndWait();
            try {
                Pane pane = FXMLLoader.load(getClass().getResource("/login/FXMLLogin.fxml"));
                nicknameField.getScene().setRoot(pane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo registrar el usuario");
            alert.setContentText("Revisa que los campos cumplan con los requisitos de la librería.");
            alert.showAndWait();
        }
    }

    @FXML
    private void seleccionaAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Foto de Perfil");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.png", "*.jpeg")
        );
        File archivo = fc.showOpenDialog(nicknameField.getScene().getWindow());
        if (archivo != null) {
            rutaAvatarRegistro = archivo.getAbsolutePath();
            Image nuevaImagen = new Image(archivo.toURI().toString());
            avatarPreview.setImage(nuevaImagen);
        }
    }

    @FXML
    private void auntenticarse(ActionEvent event) {
    }

    @FXML
    private void cancel(ActionEvent event) {
    }
}