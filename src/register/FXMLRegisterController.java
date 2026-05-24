package register;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

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
    @FXML private ImageView avatarPreview;

    private String rutaAvatarRegistro = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            Stage stage = (Stage) nicknameField.getScene().getWindow();
            stage.setMinWidth(660);
            stage.setMinHeight(580);
        });
    }

    @FXML
    private void register(ActionEvent event) {
        // 1. Ocultar todos los errores antes de validar
        nicknameError.setVisible(false);
        emailError.setVisible(false);
        passwordError.setVisible(false);
        passwordConfirmError.setVisible(false);
        dateError.setVisible(false);

        String nick  = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();
        String pass2 = password2Field.getText();

        boolean valido = true;

        // 2. Validar campo a campo con los métodos de la librería
        if (!User.checkNickName(nick)) {
            nicknameError.setVisible(true);
            valido = false;
        }
        if (!User.checkEmail(email)) {
            emailError.setVisible(true);
            valido = false;
        }
        if (!User.checkPassword(pass)) {
            passwordError.setVisible(true);
            valido = false;
        }
        if (!pass.equals(pass2)) {
            passwordConfirmError.setVisible(true);
            valido = false;
        }
        if (dateField.getValue() == null || !User.isOlderThan(dateField.getValue(), 12)) {
            dateError.setVisible(true);
            valido = false;
        }

        if (!valido) return;

        // 3. Intentar registrar
        boolean ok = SportActivityApp.getInstance().registerUser(
                nick, email, pass, dateField.getValue(), rutaAvatarRegistro);

        if (ok) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registro completado");
            alert.setHeaderText("¡Usuario creado correctamente!");
            alert.setContentText("Ya puedes iniciar sesión con tu cuenta.");
            alert.showAndWait();
            irAlLogin();
        } else {
            // Único caso que llega aquí: nickname ya en uso (todos los demás los filtramos antes)
            nicknameError.setText("Ese nickname ya está en uso");
            nicknameError.setVisible(true);
        }
    }

    @FXML
    private void seleccionaAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar foto de perfil");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.png", "*.jpeg")
        );
        File archivo = fc.showOpenDialog(nicknameField.getScene().getWindow());
        if (archivo != null) {
            rutaAvatarRegistro = archivo.getAbsolutePath();
            avatarPreview.setImage(new Image(archivo.toURI().toString()));
        }
    }

    @FXML
    private void auntenticarse(ActionEvent event) {
        irAlLogin();
    }

    @FXML
    private void cancel(ActionEvent event) {
        irAlLogin();
    }

    private void irAlLogin() {
        try {
            Pane pane = FXMLLoader.load(getClass().getResource("/login/FXMLLogin.fxml"));
            nicknameField.getScene().setRoot(pane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
