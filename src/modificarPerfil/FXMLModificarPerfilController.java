package modificarPerfil;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class FXMLModificarPerfilController implements Initializable {
    
    private final SportActivityApp app = SportActivityApp.getInstance();
    @FXML
    private PasswordField newPassword;
    @FXML
    private TextField newNickname;
    @FXML
    private DatePicker newDate;
    @FXML
    private TextField newMail;
    @FXML
    private ImageView avatar;
    
    private String rutaAvatarActual = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            Stage ventana = (Stage) newMail.getScene().getWindow();
            ventana.setMinWidth(640);
            ventana.setMinHeight(520);
        });
        User usuarioActual = app.getCurrentUser();
        if (usuarioActual != null) {
            newNickname.setText(usuarioActual.getNickName());
            newNickname.setEditable(false); 
            newMail.setText(usuarioActual.getEmail());
            
           
            newDate.setValue(usuarioActual.getBirthDate());
            
            if (usuarioActual.getAvatarPath() != null && !usuarioActual.getAvatarPath().isEmpty()) {
                try {
                    File file = new File(usuarioActual.getAvatarPath());
                    if (file.exists()) {
                        avatar.setImage(new Image(file.toURI().toString()));
                        rutaAvatarActual = usuarioActual.getAvatarPath();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }   

    @FXML
    private void cerrarSesion(ActionEvent event) {
        app.logout();
        try {
            Pane pane = new FXMLLoader(getClass().getResource("/login/FXMLLogin.fxml")).load();
            Stage ventanaPerfil = (Stage) newMail.getScene().getWindow();
            Stage ventanaPrincipal = (Stage) ventanaPerfil.getOwner();
            ventanaPrincipal.getScene().setRoot(pane);
            ventanaPerfil.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void guardar(ActionEvent event) {
        User usuarioActual = app.getCurrentUser();
        if (usuarioActual == null) return;

        String nuevoEmail = newMail.getText().trim();
        String nuevaPass = newPassword.getText();
        LocalDate nuevaFecha = newDate.getValue();

        // Si la contraseña está en blanco, mantenemos la actual
        String passDefinitiva;
        if (nuevaPass.isEmpty()) {
            passDefinitiva = usuarioActual.getPassword();
        } else {
            passDefinitiva = nuevaPass;
        }

        StringBuilder errores = new StringBuilder();

        if (!User.checkEmail(nuevoEmail)) {
            errores.append("- El formato del correo electrónico no es válido.\n");
        }
        
        // Solo validamos la contraseña si ha decidido cambiarla
        if (!nuevaPass.isEmpty() && !User.checkPassword(passDefinitiva)) {
            errores.append("- La contraseña debe tener entre 8 y 20 caracteres, mayúscula, minúscula, dígito y un símbolo.\n");
        }
        
        if (nuevaFecha == null || !User.isOlderThan(nuevaFecha, 12)) {
            errores.append("- Debes ser mayor de 12 años.\n");
        }

        if (errores.length() > 0) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al actualizar perfil");
            alerta.setHeaderText("Por favor, revisa los siguientes campos:");
            alerta.setContentText(errores.toString());
            alerta.showAndWait();
        } else {
            app.updateCurrentUser(nuevoEmail, passDefinitiva, nuevaFecha, rutaAvatarActual);
            Alert alertaExito = new Alert(Alert.AlertType.INFORMATION);
            alertaExito.setTitle("Perfil actualizado");
            alertaExito.setHeaderText(null);
            alertaExito.setContentText("¡Tus datos se han guardado correctamente!");
            alertaExito.showAndWait();
            Stage ventanaActual = (Stage) newMail.getScene().getWindow();
            ventanaActual.close();
        }
    }
    
    @FXML
    private void cambiarAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar nuevo Avatar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.png", "*.jpeg"));
        File archivo = fc.showOpenDialog(avatar.getScene().getWindow());
        if (archivo != null) {
            rutaAvatarActual = archivo.getAbsolutePath();
            avatar.setImage(new Image(archivo.toURI().toString()));
        }
    }

    @FXML
    private void cancel1(ActionEvent event) {
        Stage ventanaPerfil = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ventanaPerfil.close();
    }

}
