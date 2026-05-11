package view;

import controller.RestaurantReservationController;
import javafx.application.Application;
import javafx.stage.Stage;
import service.DemoDataFactory;

/**
 * JavaFX Application subclass that builds the first screen and owns the stage.
 */
public class RestaurantReservationApplication extends Application {
    @Override
    public void start(Stage stage) {
        RestaurantReservationController controller = new RestaurantReservationController(
                stage,
                DemoDataFactory.createDemoManager()
        );
        controller.showLoginScreen();
    }
}
