package com.parkingsystem.view;

import com.parkingsystem.controller.ParkingController;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainParkingApp {
  
    public static void main(String[] args) {     
        Application.launch(FxApp.class, args);
    }  

    public static class FxApp extends Application {   
        @Override
        public void start(Stage stage) {
            ParkingController controller = new ParkingController();
            new ParkingView(stage, controller);
            stage.show();
        }
    }
}
