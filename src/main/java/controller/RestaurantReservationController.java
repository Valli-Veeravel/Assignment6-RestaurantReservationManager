package controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.Customer;
import model.CustomerNotification;
import model.Reservation;
import model.ReservationStatus;
import model.Restaurant;
import model.Review;
import model.Staff;
import service.ReservationManager;
import service.WaitlistRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates JavaFX screens and delegates business decisions to ReservationManager.
 */
public class RestaurantReservationController {
    private static final List<LocalTime> RESERVATION_TIMES = List.of(
            LocalTime.of(17, 0),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0),
            LocalTime.of(20, 0),
            LocalTime.of(21, 0)
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final Stage stage;
    private final BorderPane root;
    private final ReservationManager reservationManager;
    private Customer currentCustomer;
    private Staff currentStaff;

    public RestaurantReservationController(Stage stage, ReservationManager reservationManager) {
        this.stage = stage;
        this.reservationManager = reservationManager;
        this.root = new BorderPane();
        this.root.getStyleClass().add("screen");
    }

    public void showLoginScreen() {
        currentCustomer = null;
        currentStaff = null;

        Label title = new Label("Restaurant Reservation Manager");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Choose a role and enter your email and password.");
        subtitle.getStyleClass().add("muted");

        RadioButton customerRole = new RadioButton("Customer Login");
        RadioButton staffRole = new RadioButton("Staff Login");
        ToggleGroup roleGroup = new ToggleGroup();
        customerRole.setToggleGroup(roleGroup);
        staffRole.setToggleGroup(roleGroup);
        customerRole.setSelected(true);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = primaryButton("Log In");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> {
            if (customerRole.isSelected()) {
                Optional<Customer> customer = reservationManager.authenticateCustomer(
                        emailField.getText(),
                        passwordField.getText()
                );
                if (customer.isEmpty()) {
                    showError("Login Error", "Invalid customer email or password.");
                    return;
                }
                currentCustomer = customer.get();
                showCustomerDashboard();
            } else {
                Optional<Staff> staff = reservationManager.authenticateStaff(
                        emailField.getText(),
                        passwordField.getText()
                );
                if (staff.isEmpty()) {
                    showError("Login Error", "Invalid staff email or password.");
                    return;
                }
                currentStaff = staff.get();
                showStaffDashboard();
            }
        });

        HBox roleRow = new HBox(20, customerRole, staffRole);
        roleRow.setAlignment(Pos.CENTER_LEFT);

        VBox loginPanel = new VBox(14, title, subtitle, roleRow, emailField, passwordField, loginButton);
        loginPanel.getStyleClass().add("panel");
        loginPanel.setMaxWidth(460);

        VBox page = new VBox(loginPanel);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(40));
        setContent("Login", page);
    }

    private void showCustomerDashboard() {
        Label title = pageTitle("Customer Dashboard");
        Label subtitle = muted("Welcome, " + currentCustomer.getName() + ".");

        Button searchButton = primaryButton("Search / Make Reservation");
        searchButton.setOnAction(event -> showSearchReservationScreen());
        Button reservationsButton = new Button("View / Cancel My Reservations");
        reservationsButton.setOnAction(event -> showMyReservationsScreen());
        Button reviewsButton = new Button("Reviews");
        reviewsButton.setOnAction(event -> showReviewsScreen());
        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> showLoginScreen());

        TableView<Reservation> table = createReservationTable();
        table.setItems(FXCollections.observableArrayList(reservationManager.getReservationsForCustomer(currentCustomer)));
        table.setPlaceholder(new Label("No reservations yet."));
        TableView<CustomerNotification> notificationsTable = createNotificationTable();
        notificationsTable.setItems(FXCollections.observableArrayList(
                reservationManager.getNotificationsForCustomer(currentCustomer)
        ));
        notificationsTable.setPlaceholder(new Label("No notifications yet."));

        TabPane tabPane = new TabPane();
        Tab reservationsTab = new Tab("My Reservations", panel(table));
        Tab notificationsTab = new Tab("Notifications", panel(notificationsTable));
        Tab accountTab = new Tab("Account Information", createCustomerAccountInfoPanel());
        reservationsTab.setClosable(false);
        notificationsTab.setClosable(false);
        accountTab.setClosable(false);
        tabPane.getTabs().add(reservationsTab);
        tabPane.getTabs().add(notificationsTab);
        tabPane.getTabs().add(accountTab);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(notificationsTable, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        HBox actions = new HBox(10, searchButton, reservationsButton, reviewsButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(18, title, subtitle, actions, tabPane);
        content.setPadding(new Insets(22));
        setContent("Customer", withTopBar(content));
    }

    private void showSearchReservationScreen() {
        ComboBox<Restaurant> restaurantBox = restaurantComboBox();
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<LocalTime> timeBox = new ComboBox<>(FXCollections.observableArrayList(RESERVATION_TIMES));
        timeBox.setConverter(timeConverter());
        timeBox.getSelectionModel().select(LocalTime.of(18, 0));
        Spinner<Integer> partySizeSpinner = new Spinner<>(1, 20, 2);
        partySizeSpinner.setEditable(true);

        Label availabilityLabel = muted("Select a restaurant, date, time, and party size.");
        TableView<AvailabilityRow> availabilityTable = createAvailabilityTable();

        Button checkButton = primaryButton("Check Availability");
        checkButton.setOnAction(event -> handleAvailabilitySearch(
                restaurantBox,
                datePicker,
                timeBox,
                partySizeSpinner,
                availabilityLabel,
                availabilityTable
        ));

        Button reserveButton = new Button("Request Reservation");
        reserveButton.setOnAction(event -> handleReservationRequest(
                restaurantBox,
                datePicker,
                timeBox,
                partySizeSpinner,
                availabilityLabel,
                availabilityTable
        ));

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> showCustomerDashboard());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Restaurant"), 0, 0);
        form.add(restaurantBox, 1, 0);
        form.add(new Label("Date"), 0, 1);
        form.add(datePicker, 1, 1);
        form.add(new Label("Time"), 0, 2);
        form.add(timeBox, 1, 2);
        form.add(new Label("Party Size"), 0, 3);
        form.add(partySizeSpinner, 1, 3);
        form.add(new HBox(10, checkButton, reserveButton, backButton), 1, 4);

        VBox content = new VBox(
                18,
                pageTitle("Search Availability / Make Reservation"),
                panel(form, availabilityLabel),
                panel(sectionTitle("Availability Result"), availabilityTable)
        );
        content.setPadding(new Insets(22));
        VBox.setVgrow(availabilityTable, Priority.ALWAYS);
        setContent("Search", withTopBar(content));
    }

    private void showMyReservationsScreen() {
        TableView<Reservation> table = createReservationTable();
        table.setItems(FXCollections.observableArrayList(reservationManager.getReservationsForCustomer(currentCustomer)));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        Button cancelButton = dangerButton("Cancel Selected Reservation");
        cancelButton.setOnAction(event -> {
            Reservation selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Cancel Reservation", "Please select a reservation to cancel.");
                return;
            }
            if (selected.getStatus() == ReservationStatus.CANCELLED || selected.getStatus() == ReservationStatus.DENIED) {
                showError("Cancel Reservation", "Only pending or accepted reservations can be cancelled.");
                return;
            }
            Optional<ButtonType> result = confirm(
                    "Cancel Reservation",
                    "Cancel your reservation at " + selected.getRestaurant().getName() + "?"
            );
            if (result.filter(ButtonType.OK::equals).isPresent()) {
                try {
                    reservationManager.cancelReservation(selected);
                    table.setItems(FXCollections.observableArrayList(
                            reservationManager.getReservationsForCustomer(currentCustomer)
                    ));
                    showInfo("Reservation Cancelled", "Your reservation was cancelled.");
                } catch (RuntimeException ex) {
                    showError("Cancel Reservation", ex.getMessage());
                }
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> showCustomerDashboard());

        VBox content = new VBox(
                18,
                pageTitle("View and Cancel My Reservations"),
                panel(table),
                new HBox(10, cancelButton, backButton)
        );
        content.setPadding(new Insets(22));
        VBox.setVgrow(table, Priority.ALWAYS);
        setContent("Reservations", withTopBar(content));
    }

    private void showStaffDashboard() {
        Label title = pageTitle("Staff Dashboard");
        String restaurantName = currentStaff.getRestaurant() == null
                ? "No assigned restaurant"
                : currentStaff.getRestaurant().getName();
        Label subtitle = muted("Signed in as " + currentStaff.getName()
                + " (" + currentStaff.getRole() + ") - " + restaurantName + ".");
        TableView<Reservation> pendingTable = createReservationTable();
        pendingTable.setItems(FXCollections.observableArrayList(
                reservationManager.getPendingReservationsForStaff(currentStaff)
        ));
        pendingTable.setPlaceholder(new Label("No pending reservation requests."));
        TableView<Reservation> currentReservationsTable = createReservationTable();
        currentReservationsTable.setItems(FXCollections.observableArrayList(
                reservationManager.getCurrentReservationsForStaff(currentStaff)
        ));
        currentReservationsTable.setPlaceholder(new Label("No accepted reservations."));
        TableView<WaitlistRecord> waitlistTable = createWaitlistTable();
        waitlistTable.setItems(FXCollections.observableArrayList(
                reservationManager.getWaitlistRecordsForStaff(currentStaff)
        ));
        waitlistTable.setPlaceholder(new Label("No waitlisted customers."));

        Button acceptButton = successButton("Accept");
        acceptButton.setOnAction(event -> handleStaffDecision(pendingTable, currentReservationsTable, true));
        Button denyButton = dangerButton("Deny");
        denyButton.setOnAction(event -> handleStaffDecision(pendingTable, currentReservationsTable, false));
        Button reviewsButton = new Button("Reviews");
        reviewsButton.setOnAction(event -> showReviewsScreen());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshStaffTables(pendingTable, currentReservationsTable, waitlistTable));
        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> showLoginScreen());

        HBox actions = new HBox(10, acceptButton, denyButton, reviewsButton, refreshButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        TabPane staffTabs = new TabPane();
        Tab pendingTab = new Tab("Pending Requests", panel(pendingTable));
        Tab currentTab = new Tab("Current Reservations", panel(currentReservationsTable));
        Tab waitlistTab = new Tab("Waitlist", panel(waitlistTable));
        Tab accountTab = new Tab("Account Information", createStaffAccountInfoPanel());
        pendingTab.setClosable(false);
        currentTab.setClosable(false);
        waitlistTab.setClosable(false);
        accountTab.setClosable(false);
        staffTabs.getTabs().add(pendingTab);
        staffTabs.getTabs().add(currentTab);
        staffTabs.getTabs().add(waitlistTab);
        staffTabs.getTabs().add(accountTab);

        VBox content = new VBox(
                18,
                title,
                subtitle,
                staffTabs,
                actions
        );
        content.setPadding(new Insets(22));
        VBox.setVgrow(staffTabs, Priority.ALWAYS);
        setContent("Staff", withTopBar(content));
    }

    private void showReviewsScreen() {
        ComboBox<Restaurant> restaurantBox = restaurantComboBox();
        Spinner<Integer> ratingSpinner = new Spinner<>(1, 5, 5);
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write a short review...");
        commentArea.setPrefRowCount(4);
        TableView<Review> reviewTable = createReviewTable();
        refreshReviews(reviewTable, restaurantBox.getSelectionModel().getSelectedItem());

        restaurantBox.setOnAction(event -> refreshReviews(reviewTable, restaurantBox.getSelectionModel().getSelectedItem()));

        Button submitButton = primaryButton("Submit Review");
        submitButton.setDisable(currentCustomer == null);
        submitButton.setOnAction(event -> {
            try {
                Restaurant restaurant = restaurantBox.getSelectionModel().getSelectedItem();
                reservationManager.submitReview(currentCustomer, restaurant, ratingSpinner.getValue(), commentArea.getText());
                commentArea.clear();
                refreshReviews(reviewTable, restaurant);
                showInfo("Review Submitted", "Thank you for leaving a review.");
            } catch (RuntimeException ex) {
                showError("Review Error", ex.getMessage());
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> {
            if (currentStaff != null) {
                showStaffDashboard();
            } else {
                showCustomerDashboard();
            }
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Restaurant"), 0, 0);
        form.add(restaurantBox, 1, 0);
        form.add(new Label("Rating"), 0, 1);
        form.add(ratingSpinner, 1, 1);
        form.add(new Label("Comment"), 0, 2);
        form.add(commentArea, 1, 2);
        form.add(new HBox(10, submitButton, backButton), 1, 3);

        if (currentCustomer == null) {
            form.add(muted("Staff can view reviews. Log in as a customer to submit one."), 1, 4);
        }

        VBox content = new VBox(
                18,
                pageTitle("Reviews"),
                panel(form),
                panel(sectionTitle("Restaurant Reviews"), reviewTable)
        );
        content.setPadding(new Insets(22));
        VBox.setVgrow(reviewTable, Priority.ALWAYS);
        setContent("Reviews", withTopBar(content));
    }

    private void handleAvailabilitySearch(
            ComboBox<Restaurant> restaurantBox,
            DatePicker datePicker,
            ComboBox<LocalTime> timeBox,
            Spinner<Integer> partySizeSpinner,
            Label availabilityLabel,
            TableView<AvailabilityRow> availabilityTable
    ) {
        try {
            Restaurant restaurant = restaurantBox.getSelectionModel().getSelectedItem();
            LocalDateTime dateTime = selectedDateTime(datePicker, timeBox);
            int partySize = partySizeSpinner.getValue();
            boolean available = reservationManager.searchAvailability(restaurant, dateTime, partySize);
            int capacity = restaurant.getAvailabilitySchedule().getCapacity(dateTime);
            availabilityLabel.setText(available
                    ? "Available. You can submit a reservation request."
                    : "No availability for that party size. You can join the waitlist.");
            availabilityTable.setItems(FXCollections.observableArrayList(
                    new AvailabilityRow(restaurant.getName(), format(dateTime), partySize, capacity, available ? "Available" : "Waitlist")
            ));
        } catch (RuntimeException ex) {
            showError("Availability Search", ex.getMessage());
        }
    }

    private void handleReservationRequest(
            ComboBox<Restaurant> restaurantBox,
            DatePicker datePicker,
            ComboBox<LocalTime> timeBox,
            Spinner<Integer> partySizeSpinner,
            Label availabilityLabel,
            TableView<AvailabilityRow> availabilityTable
    ) {
        try {
            Restaurant restaurant = restaurantBox.getSelectionModel().getSelectedItem();
            LocalDateTime dateTime = selectedDateTime(datePicker, timeBox);
            int partySize = partySizeSpinner.getValue();

            // Confirm waitlist placement in the UI, but keep eligibility checks in the service layer.
            if (!reservationManager.searchAvailability(restaurant, dateTime, partySize)) {
                Optional<ButtonType> result = confirm(
                        "Join Waitlist",
                        "No tables are available for this time. Add yourself to the waitlist?"
                );
                if (result.filter(ButtonType.OK::equals).isPresent()) {
                    reservationManager.joinWaitlist(currentCustomer, restaurant, dateTime, partySize);
                    availabilityLabel.setText("You have been added to the waitlist.");
                    showInfo("Waitlist", "You were added to the waitlist.");
                }
                return;
            }

            Optional<Reservation> reservation = reservationManager.createRequest(
                    currentCustomer,
                    restaurant,
                    dateTime,
                    partySize
            );
            if (reservation.isPresent()) {
                showInfo("Reservation Requested", "Your reservation is pending staff approval.");
                handleAvailabilitySearch(restaurantBox, datePicker, timeBox, partySizeSpinner, availabilityLabel, availabilityTable);
            }
        } catch (RuntimeException ex) {
            showError("Reservation Request", ex.getMessage());
        }
    }

    private void handleStaffDecision(
            TableView<Reservation> pendingTable,
            TableView<Reservation> currentReservationsTable,
            boolean accept
    ) {
        Reservation selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Pending Reservation", "Please select a pending reservation.");
            return;
        }

        String action = accept ? "accept" : "deny";
        Optional<ButtonType> result = confirm(
                "Confirm Reservation",
                "Are you sure you want to " + action + " this reservation?"
        );
        if (result.filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            // Staff actions go through the domain/service API so status rules stay out of the UI.
            if (accept) {
                currentStaff.approveReservation(reservationManager, selected.getReservationId());
            } else {
                currentStaff.denyReservation(reservationManager, selected.getReservationId());
            }
            pendingTable.setItems(FXCollections.observableArrayList(
                    reservationManager.getPendingReservationsForStaff(currentStaff)
            ));
            currentReservationsTable.setItems(FXCollections.observableArrayList(
                    reservationManager.getCurrentReservationsForStaff(currentStaff)
            ));
            showInfo("Reservation Updated", "Reservation was " + (accept ? "accepted." : "denied."));
        } catch (RuntimeException ex) {
            showError("Reservation Update", ex.getMessage());
        }
    }

    private void refreshStaffTables(
            TableView<Reservation> pendingTable,
            TableView<Reservation> currentReservationsTable,
            TableView<WaitlistRecord> waitlistTable
    ) {
        pendingTable.setItems(FXCollections.observableArrayList(
                reservationManager.getPendingReservationsForStaff(currentStaff)
        ));
        currentReservationsTable.setItems(FXCollections.observableArrayList(
                reservationManager.getCurrentReservationsForStaff(currentStaff)
        ));
        waitlistTable.setItems(FXCollections.observableArrayList(
                reservationManager.getWaitlistRecordsForStaff(currentStaff)
        ));
    }

    private VBox createCustomerAccountInfoPanel() {
        GridPane accountGrid = accountInfoGrid();
        accountGrid.add(new Label("Name"), 0, 0);
        accountGrid.add(new Label(currentCustomer.getName()), 1, 0);
        accountGrid.add(new Label("Email"), 0, 1);
        accountGrid.add(new Label(currentCustomer.getEmail()), 1, 1);
        accountGrid.add(new Label("Phone"), 0, 2);
        accountGrid.add(new Label(currentCustomer.getPhoneNumber()), 1, 2);
        return panel(sectionTitle("Account Information"), accountGrid);
    }

    private VBox createStaffAccountInfoPanel() {
        String restaurantName = currentStaff.getRestaurant() == null
                ? "No assigned restaurant"
                : currentStaff.getRestaurant().getName();
        GridPane accountGrid = accountInfoGrid();
        accountGrid.add(new Label("Name"), 0, 0);
        accountGrid.add(new Label(currentStaff.getName()), 1, 0);
        accountGrid.add(new Label("Email"), 0, 1);
        accountGrid.add(new Label(currentStaff.getEmail()), 1, 1);
        accountGrid.add(new Label("Phone"), 0, 2);
        accountGrid.add(new Label(currentStaff.getPhoneNumber()), 1, 2);
        accountGrid.add(new Label("Position"), 0, 3);
        accountGrid.add(new Label(currentStaff.getRole()), 1, 3);
        accountGrid.add(new Label("Restaurant"), 0, 4);
        accountGrid.add(new Label(restaurantName), 1, 4);
        return panel(sectionTitle("Account Information"), accountGrid);
    }

    private GridPane accountInfoGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        return grid;
    }

    private TableView<Reservation> createReservationTable() {
        TableView<Reservation> table = new TableView<>();

        TableColumn<Reservation, String> restaurantColumn = new TableColumn<>("Restaurant");
        restaurantColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRestaurant().getName()));
        restaurantColumn.setPrefWidth(180);

        TableColumn<Reservation, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCustomer().getName()));
        customerColumn.setPrefWidth(160);

        TableColumn<Reservation, String> timeColumn = new TableColumn<>("Date / Time");
        timeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(format(data.getValue().getDateTime())));
        timeColumn.setPrefWidth(190);

        TableColumn<Reservation, Integer> partyColumn = new TableColumn<>("Party");
        partyColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getPartySize()));
        partyColumn.setPrefWidth(80);

        TableColumn<Reservation, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStatus().name()));
        statusColumn.setPrefWidth(120);

        table.getColumns().add(restaurantColumn);
        table.getColumns().add(customerColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(partyColumn);
        table.getColumns().add(statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private TableView<AvailabilityRow> createAvailabilityTable() {
        TableView<AvailabilityRow> table = new TableView<>();
        TableColumn<AvailabilityRow, String> restaurantColumn = new TableColumn<>("Restaurant");
        restaurantColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().restaurant()));
        TableColumn<AvailabilityRow, String> timeColumn = new TableColumn<>("Date / Time");
        timeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().dateTime()));
        TableColumn<AvailabilityRow, Integer> partyColumn = new TableColumn<>("Party");
        partyColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().partySize()));
        TableColumn<AvailabilityRow, Integer> capacityColumn = new TableColumn<>("Open Seats");
        capacityColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().remainingCapacity()));
        TableColumn<AvailabilityRow, String> resultColumn = new TableColumn<>("Result");
        resultColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().result()));
        table.getColumns().add(restaurantColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(partyColumn);
        table.getColumns().add(capacityColumn);
        table.getColumns().add(resultColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Run an availability search."));
        return table;
    }

    private TableView<Review> createReviewTable() {
        TableView<Review> table = new TableView<>();
        TableColumn<Review, String> restaurantColumn = new TableColumn<>("Restaurant");
        restaurantColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRestaurant().getName()));
        restaurantColumn.setPrefWidth(170);

        TableColumn<Review, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCustomer().getName()));
        customerColumn.setPrefWidth(150);

        TableColumn<Review, Integer> ratingColumn = new TableColumn<>("Rating");
        ratingColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getRating()));
        ratingColumn.setPrefWidth(80);

        TableColumn<Review, String> commentColumn = new TableColumn<>("Comment");
        commentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getComment()));
        commentColumn.setPrefWidth(360);

        TableColumn<Review, String> dateColumn = new TableColumn<>("Created");
        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(format(data.getValue().getCreatedAt())));
        dateColumn.setPrefWidth(170);

        table.getColumns().add(restaurantColumn);
        table.getColumns().add(customerColumn);
        table.getColumns().add(ratingColumn);
        table.getColumns().add(commentColumn);
        table.getColumns().add(dateColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No reviews yet."));
        return table;
    }

    private TableView<CustomerNotification> createNotificationTable() {
        TableView<CustomerNotification> table = new TableView<>();
        TableColumn<CustomerNotification, String> createdColumn = new TableColumn<>("Date / Time");
        createdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(format(data.getValue().getCreatedAt())));
        createdColumn.setPrefWidth(190);

        TableColumn<CustomerNotification, String> messageColumn = new TableColumn<>("Notification");
        messageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getMessage()));
        messageColumn.setPrefWidth(650);

        table.getColumns().add(createdColumn);
        table.getColumns().add(messageColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private TableView<WaitlistRecord> createWaitlistTable() {
        TableView<WaitlistRecord> table = new TableView<>();
        TableColumn<WaitlistRecord, String> restaurantColumn = new TableColumn<>("Restaurant");
        restaurantColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().restaurant().getName()));
        TableColumn<WaitlistRecord, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().entry().getCustomer().getName()));
        TableColumn<WaitlistRecord, String> contactColumn = new TableColumn<>("Contact");
        contactColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().entry().getCustomer().getPhoneNumber()));
        TableColumn<WaitlistRecord, String> timeColumn = new TableColumn<>("Requested Time");
        timeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(format(data.getValue().entry().getRequestedDateTime())));
        TableColumn<WaitlistRecord, Integer> partyColumn = new TableColumn<>("Party");
        partyColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().entry().getPartySize()));

        table.getColumns().add(restaurantColumn);
        table.getColumns().add(customerColumn);
        table.getColumns().add(contactColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(partyColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private void refreshReviews(TableView<Review> reviewTable, Restaurant restaurant) {
        ObservableList<Review> reviews = restaurant == null
                ? FXCollections.observableArrayList(reservationManager.getReviews())
                : FXCollections.observableArrayList(reservationManager.getReviewsForRestaurant(restaurant));
        reviewTable.setItems(reviews);
    }

    private ComboBox<Restaurant> restaurantComboBox() {
        ComboBox<Restaurant> restaurantBox = new ComboBox<>(
                FXCollections.observableArrayList(reservationManager.getRestaurants())
        );
        restaurantBox.setConverter(restaurantConverter());
        restaurantBox.getSelectionModel().selectFirst();
        restaurantBox.setMaxWidth(Double.MAX_VALUE);
        return restaurantBox;
    }

    private LocalDateTime selectedDateTime(DatePicker datePicker, ComboBox<LocalTime> timeBox) {
        LocalDate date = datePicker.getValue();
        LocalTime time = timeBox.getSelectionModel().getSelectedItem();
        if (date == null || time == null) {
            throw new IllegalArgumentException("Date and time are required.");
        }
        return LocalDateTime.of(date, time);
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private Node withTopBar(Node content) {
        BorderPane layout = new BorderPane();
        layout.setTop(topBar());
        layout.setCenter(content);
        return layout;
    }

    private Node topBar() {
        Label title = new Label("Restaurant Reservation Manager");
        title.getStyleClass().add("top-title");
        Label subtitle = new Label("Reservations, waitlists, staff approvals, and reviews");
        subtitle.getStyleClass().add("top-subtitle");
        VBox text = new VBox(2, title, subtitle);
        HBox bar = new HBox(text);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void setContent(String title, Node content) {
        root.setCenter(content);
        if (stage.getScene() == null) {
            // Reuse one Scene and swap the center content for simple screen navigation.
            Scene scene = new Scene(root, 1100, 720);
            String stylesheet = getClass().getResource("/styles/app.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
            stage.setScene(scene);
        }
        stage.setTitle("Restaurant Reservation Manager - " + title);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
    }

    private Label pageTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private VBox panel(Node... nodes) {
        VBox box = new VBox(12, nodes);
        box.getStyleClass().add("panel");
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        return button;
    }

    private Button successButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("success-button");
        return button;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait();
    }

    private StringConverter<Restaurant> restaurantConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Restaurant restaurant) {
                return restaurant == null ? "" : restaurant.getName();
            }

            @Override
            public Restaurant fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<Customer> customerConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<Staff> staffConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Staff staff) {
                if (staff == null) {
                    return "";
                }
                String restaurantName = staff.getRestaurant() == null
                        ? "No assigned restaurant"
                        : staff.getRestaurant().getName();
                return staff.getName() + " (" + staff.getRole() + " - " + restaurantName + ")";
            }

            @Override
            public Staff fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<LocalTime> timeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.format(DateTimeFormatter.ofPattern("h:mm a"));
            }

            @Override
            public LocalTime fromString(String string) {
                return null;
            }
        };
    }

    private record AvailabilityRow(
            String restaurant,
            String dateTime,
            int partySize,
            int remainingCapacity,
            String result
    ) {
    }
}
