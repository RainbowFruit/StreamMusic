package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Music receiver");
        Scene scene = new Scene(root, 500, 440);
        primaryStage.setScene(scene);
        primaryStage.show();

        StaticFields.myController = loader.getController();
    }


    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
