package mapademo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MapaDemoApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/login/FXMLLogin.fxml"));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        Scene scene = new Scene(root);
        stage.setTitle("Running la Safor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}
