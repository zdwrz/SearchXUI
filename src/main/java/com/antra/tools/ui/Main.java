package com.antra.tools.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        String version = getParameters().getNamed().get("version");
        Parent root = null;
        if (version == null || version.equals("new")) {
            root = FXMLLoader.load(getClass().getResource("/mainRT.fxml"));
            primaryStage.setTitle("Search From Files - RT");
        }else if ("old".equals(version)){
            root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            primaryStage.setTitle("Search From Files");
        }

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
