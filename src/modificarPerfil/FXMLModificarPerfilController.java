/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package modificarPerfil;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

/**
 * FXML Controller class
 *
 * @author editor
 */
public class FXMLModificarPerfilController implements Initializable {
    
    private final SportActivityApp app = SportActivityApp.getInstance();
    @FXML
    private Label passwordError;
    @FXML
    private PasswordField newPassword;
    @FXML
    private TextField newNickname;
    @FXML
    private DatePicker newDate;
    @FXML
    private Button newAvatar;
    @FXML
    private TextField newMail;
    @FXML
    private ImageView avatar;
    
    private String rutaAvatarActual;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void cerrarSesion(ActionEvent event) {
        app.logout();
        try {
            Pane pane = new FXMLLoader(getClass().getResource("/login/FXMLLogin.fxml")).load();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void guardar(ActionEvent event) {
        User usuarioActual = app.getCurrentUser();
        if (usuarioActual == null) return;

        String nuevoEmail = newMail.getText().trim();
        String nuevaPass = newPassword.getText();
        LocalDate nuevaFecha = newDate.getValue();

        // 1. REGLA DE LA CONTRASEÑA: Si está en blanco, mantenemos la actual sin modificarla
        String passDefinitiva;
        if (nuevaPass.isEmpty()) {
            passDefinitiva = usuarioActual.getPassword();
        } else {
            passDefinitiva = nuevaPass;
        }

        // 2. VALIDACIONES DE LA LIBRERÍA
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

        // 3. COMPROBAR RESULTADO Y GUARDAR
        if (errores.length() > 0) {
            // Mostrar alerta de error
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al actualizar perfil");
            alerta.setHeaderText("Por favor, revisa los siguientes campos:");
            alerta.setContentText(errores.toString());
            alerta.showAndWait();
        } else {
            // Si todo es correcto, actualizamos los datos en la base de datos
            app.updateCurrentUser(nuevoEmail, passDefinitiva, nuevaFecha, rutaAvatarActual);
            
            // Mostrar mensaje de éxito
            Alert alertaExito = new Alert(Alert.AlertType.INFORMATION);
            alertaExito.setTitle("Perfil actualizado");
            alertaExito.setHeaderText(null);
            alertaExito.setContentText("¡Tus datos se han guardado correctamente!");
            alertaExito.showAndWait();

            // Cerrar la ventana modal
            Stage ventanaActual = (Stage) newMail.getScene().getWindow();
            ventanaActual.close();
        }
    }
    
    // (Opcional) Método para el botón de "Cambiar Avatar"
    @FXML
    private void cambiarAvatar(ActionEvent event) {
        // 1. CREAMOS EL FILECHOOSER POR CÓDIGO
    FileChooser fc = new FileChooser();
    fc.setTitle("Seleccionar nuevo Avatar");
    
    // Filtro para que solo pueda elegir imágenes
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.png", "*.jpeg"));
    
    // 2. ABRIMOS LA VENTANA DEL SISTEMA OPERATIVO
    File archivo = fc.showOpenDialog(avatar.getScene().getWindow());
    
    // 3. SI EL USUARIO ELIGE UNA FOTO...
    if (archivo != null) {
        // Guardamos la ruta temporalmente
        rutaAvatarActual = archivo.getAbsolutePath();
        
        // Cargamos la foto en el ImageView para que el usuario la vea
        Image nuevaImagen = new Image(archivo.toURI().toString());
        avatar.setImage(nuevaImagen);
    }
    }

    @FXML
    private void cancel1(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void seleccionaFechaNacimiento(ActionEvent event) {
    }

    @FXML
    private void seleccionaAvatar(ActionEvent event) {
    } 
}
