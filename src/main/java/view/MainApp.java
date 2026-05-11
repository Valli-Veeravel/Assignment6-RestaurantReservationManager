package view;

import controller.RestaurantReservationController;
import javafx.application.Application;
import javafx.stage.Stage;
import service.DemoDataFactory;

/**
 * JavaFX entry point for the restaurant reservation application.
 */
public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        RestaurantReservationController controller = new RestaurantReservationController(
                stage,
                DemoDataFactory.createDemoManager()
        );
        controller.showLoginScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
