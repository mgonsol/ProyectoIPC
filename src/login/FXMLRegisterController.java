/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package login;


import java.net.URL;

import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.time.LocalDate;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;


public class FXMLRegisterController implements Initializable {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField password2Field;
    @FXML
    private DatePicker dateField;
    
    private BooleanProperty validEmail;
    private BooleanProperty validPassword;
    private BooleanProperty confirmPasswords;
    private BooleanProperty validDate;
    
    private javafx.beans.value.ChangeListener<String> listenerEmail;
    private javafx.beans.value.ChangeListener<String> listenerPassword;
    private javafx.beans.value.ChangeListener<String> listenerPassword2;
    private javafx.beans.value.ChangeListener<LocalDate> listenerDate;
    
    @FXML
    private Label emailError;
    @FXML
    private Label passwordError;
    @FXML
    private Label passwordConfirmError;
    @FXML
    private Label dateError;
    @FXML
    private Button bAccept;
    @FXML
    private Button bCancel;

    
    private void showError(boolean isValid, Node field, Node errorMessage){
        errorMessage.setVisible(!isValid);
        field.setStyle(((isValid) ? "" : "-fx-background-color: #FCE5E0"));
    }
    
    private void checkEmail() {
        String email = emailField.getText();
        boolean isValid = email.matches("^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");
        validEmail.set(isValid); //actualiza la property asociada
        showError(isValid, emailField, emailError); //muestra o esconde el mensaje de error
    }
    
    private void checkPassword() {
        String password = passwordField.getText();
        boolean isValid = password.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,15}$");
        validPassword.set(isValid); //actualiza la property asociada
        showError(isValid, passwordField, passwordError); //muestra o esconde el mensaje de error
    }
    
    private void checkPasswordsMatch() {
        boolean match = passwordField.getText().equals(password2Field.getText());
        confirmPasswords.set(match);
        showError(match, password2Field, passwordConfirmError);
    }
    
    private void checkDate(){
        LocalDate value = dateField.getValue();
        boolean isValid = value.isBefore(LocalDate.now().minusYears(16));
        validDate.set(isValid);
        showError(isValid, dateField, dateError);
    }

    //=========================================================
    // you must initialize here all related with the object 
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
    private void handleBAcceptOnAction(ActionEvent event) {
        
        emailField.clear();
        passwordField.clear();
        password2Field.clear();
        dateField.setValue(null);

        validEmail.setValue(Boolean.FALSE);
        validPassword.setValue(Boolean.FALSE);
        validDate.setValue(Boolean.FALSE);
    }
}