/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package register;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
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
public class FXMLRegisterController implements Initializable {

    @FXML
    private TextField emailField;
    @FXML
    private Label emailError;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordError;
    private Button bAccept;
    private Button bCancel;
    @FXML
    private PasswordField password2Field;
    @FXML
    private Label passwordConfirmError;
    @FXML
    private DatePicker dateField;
    @FXML
    private Label dateError;
    
    private BooleanProperty validEmail;
    private BooleanProperty validPassword;
    private BooleanProperty confirmPasswords;
    private BooleanProperty validDate;
    
    private javafx.beans.value.ChangeListener<String> listenerEmail;
    private javafx.beans.value.ChangeListener<String> listenerPassword;
    private javafx.beans.value.ChangeListener<String> listenerPassword2;
    private javafx.beans.value.ChangeListener<LocalDate> listenerDate;
    @FXML
    private TextField nicknameField;
    
    SportActivityApp app = SportActivityApp.getInstance();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
            validEmail = new SimpleBooleanProperty();
            validEmail.setValue(Boolean.FALSE);
            
            emailField.focusedProperty().addListener((observable, oldValue, newValue)->{
                if(!newValue){
                    checkEmail();
                    if(!validEmail.get()){
                        if(listenerEmail == null){
                            listenerEmail = (a, b, c) -> checkEmail();
                            emailField.textProperty().addListener(listenerEmail);
                        }
                    }
                }
            });
            
            validPassword = new SimpleBooleanProperty();
            validPassword.setValue(Boolean.FALSE);
            
            passwordField.focusedProperty().addListener((observable, oldValue, newValue)->{
                if(!newValue){
                    checkPassword();
                    if(!validPassword.get()){
                        if(listenerPassword == null){
                            listenerPassword = (a, b, c) -> checkPassword();
                            passwordField.textProperty().addListener(listenerPassword);
                        }
                    }
                }
            });
            
            confirmPasswords = new SimpleBooleanProperty();
            confirmPasswords.setValue(Boolean.FALSE);
            
            password2Field.focusedProperty().addListener((observable, oldValue, newValue)->{
                if(!newValue){
                    checkPasswordsMatch();
                    if(!confirmPasswords.get()){
                        if(listenerPassword2 == null){
                            listenerPassword2 = (a, b, c) -> checkPasswordsMatch();
                            password2Field.textProperty().addListener(listenerPassword2);
                        }
                    }
                }
            });
            
            validDate = new SimpleBooleanProperty();
            validDate.setValue(Boolean.FALSE);
            
            dateField.focusedProperty().addListener((observable, oldValue, newValue)->{
                if(!newValue){
                    checkDate();
                    if(!validDate.get()){
                        if(listenerDate == null){
                            listenerDate = (a, b, c) -> checkDate();
                            dateField.valueProperty().addListener(listenerDate);
                        }
                    }
                }
            });
            
            BooleanBinding validFields = Bindings.and(validEmail, validPassword)
                .and(validDate);

            bAccept.disableProperty().bind(
                Bindings.not(validFields)
                );
            
            bCancel.setOnAction( (event)->{
                bCancel.getScene().getWindow().hide();
                });
 
 
    }   

    @FXML
    private void cancel(ActionEvent event) {
        System.exit(0);
    }
    private void showError(boolean isValid, Node field, Node errorMessage){
        errorMessage.setVisible(!isValid);
        field.setStyle(((isValid) ? "" : "-fx-background-color: #FCE5E0"));
    }
    
    private void checkEmail() {
        User.checkEmail(emailField.getText());
    }
    
    private void checkNickname(){
        User.checkNickName(nicknameField.getText());
    }
    
    private void checkPassword() {
        User.checkPassword(passwordField.getText());
    }
    
    private void checkPasswordsMatch() {
        boolean match = passwordField.getText().equals(password2Field.getText());
        confirmPasswords.set(match);
        showError(match, password2Field, passwordConfirmError);
    }
    
    private void checkDate(){
        User.isOlderThan(dateField.getValue(), 12);
    }

    @FXML
    private void register(ActionEvent event) {
        app.registerUser(nicknameField.getText(), emailField.getText(), passwordField.getText(), dateField.getValue(), "");
    }

    @FXML
    private void auntenticarse(ActionEvent event) {
       
    }
        

}
