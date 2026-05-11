package view;

import javafx.application.Application;

/**
 * Plain Java launcher for the restaurant reservation application.
 *
 * Keeping this class separate from the JavaFX Application subclass makes it
 * easier to run MainApp directly from IDEs that do not automatically configure
 * JavaFX module launch options.
 */
public class MainApp {
    public static void main(String[] args) {
        Application.launch(RestaurantReservationApplication.class, args);
    }
}
