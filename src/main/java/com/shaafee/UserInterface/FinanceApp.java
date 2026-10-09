package com.shaafee.UserInterface;

import com.shaafee.TransactionStorage.DataStorage;
import com.shaafee.controller.BudgetController;
import com.shaafee.controller.TransactionController;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.BudgetService;
import com.shaafee.service.TransactionService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * JavaFX window for the finance tracker. It only talks to your existing
 * controllers, so services, storage and tests stay untouched.
 *
 * All colours and fonts live in src/main/resources/finance.css. If that file
 * is missing the app still works, it just uses JavaFX's default look.
 */
public class FinanceApp extends Application {

    private static final String STYLESHEET = "/finance.css";

    // ---------- controllers (created in start) ----------
    private TransactionController transactionController;
    private BudgetController budgetController;

    // ---------- shared state ----------
    private static String stylesheetUrl;   // null if finance.css was not found
    private YearMonth currentMonth = YearMonth.now();
    private final Label monthLabel = new Label();
    private final CheckBox allMonths = new CheckBox("Show all months");

    // ---------- transactions tab ----------
    private final ObservableList<Transaction> transactionRows = FXCollections.observableArrayList();
    private final TableView<Transaction> transactionTable = new TableView<>(transactionRows);
    private final ComboBox<TransactionType> typeBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField descriptionField = new TextField();
    private final TextField amountField = new TextField();
    private final Button updateButton = new Button("Update selected");
    private final Button deleteButton = new Button("Delete selected");

    // ---------- budgets tab ----------
    private record BudgetRow(String category, double limit, double spent) {
        double remaining() {
            return limit - spent;
        }
    }

    private final ObservableList<BudgetRow> budgetRows = FXCollections.observableArrayList();
    private final TableView<BudgetRow> budgetTable = new TableView<>(budgetRows);
    private final Label budgetTitle = new Label();
    private final TextField budgetCategoryField = new TextField();
    private final TextField budgetLimitField = new TextField();

    // ---------- summary tab ----------
    private final Label incomeLabel = new Label();
    private final Label expenseLabel = new Label();
    private final Label balanceLabel = new Label();
    private final PieChart expenseChart = new PieChart();

    // =====================================================================
    // start-up
    // =====================================================================

    @Override
    public void start(Stage stage) {
        var css = FinanceApp.class.getResource(STYLESHEET);
        stylesheetUrl = (css == null) ? null : css.toExternalForm();

        try {
            setUpControllers();
        } catch (RuntimeException e) {
            showError("Could not open the database: " + e.getMessage());
            Platform.exit();
            return;
        }

        TabPane tabs = new TabPane(
                tab("Transactions", buildTransactionsTab()),
                tab("Budgets", buildBudgetsTab()),
                tab("Summary", buildSummaryTab()));

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(tabs);

        refreshAll();

        Scene scene = new Scene(root, 1000, 700);
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl);
        }
        var icon = FinanceApp.class.getResource("/logo.jpeg");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }

        stage.setTitle("Finance Tracker");
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    // same wiring as Main.run(), just without the console View
    private void setUpControllers() {
        DataStorage storage = new DataStorage(Path.of("data", "finance.db"));

        TransactionService transactionService = new TransactionService();
        BudgetService budgetService = new BudgetService();

        for (Transaction t : storage.loadTransactions()) {
            transactionService.addTransaction(t);
        }
        for (Budget b : storage.loadBudgets()) {
            budgetService.setBudget(b);
        }

        Runnable saveAll = () ->
                storage.save(transactionService.getTransactions(), budgetService.getBudgets());

        transactionController = new TransactionController(transactionService, saveAll);
        budgetController = new BudgetController(budgetService, transactionService, saveAll);
    }

    // =====================================================================
    // header: title on the left, month picker on the right (applies to every tab)
    // =====================================================================

    private HBox buildHeader() {
        Label title = new Label("Finance Tracker");
        title.getStyleClass().add("app-title");

        Button prev = new Button("<");
        Button next = new Button(">");
        Button today = new Button("This month");
        prev.getStyleClass().add("nav-button");
        next.getStyleClass().add("nav-button");

        prev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshAll();
        });
        next.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshAll();
        });
        today.setOnAction(e -> {
            currentMonth = YearMonth.now();
            refreshAll();
        });

        monthLabel.getStyleClass().add("month-label");

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = logoView(36);
        if (logo != null) {
            brand.getChildren().add(logo);
        }
        brand.getChildren().add(title);

        HBox header = new HBox(10, brand, spacer(), prev, monthLabel, next, today);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }
    //build logo
    private static ImageView logoView(double height) {
        var url = FinanceApp.class.getResource("/logo.jpeg");
        if (url == null) {
            return null;   // no logo file: skip it
        }
        ImageView view = new ImageView(new Image(url.toExternalForm()));
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    // =====================================================================
    // transactions tab
    // =====================================================================

    private VBox buildTransactionsTab() {
        typeBox.getItems().setAll(TransactionType.values());
        typeBox.setValue(TransactionType.EXPENSE);
        typeBox.setPrefWidth(140);
        datePicker.setPrefWidth(170);
        amountField.setPrefWidth(140);
        descriptionField.setPromptText("Description / category, e.g. Food");
        amountField.setPromptText("12.50");

        // Enter in the description jumps to amount; Enter in amount adds.
        // Together with "clear after add" this makes adding many items fast.
        descriptionField.setOnAction(e -> amountField.requestFocus());
        amountField.setOnAction(e -> onAddTransaction());
        allMonths.setOnAction(e -> refreshTransactions());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, captionLabel("Type"), typeBox, captionLabel("Date"), datePicker);
        form.addRow(1, captionLabel("Description"), descriptionField, captionLabel("Amount (RM)"), amountField);
        GridPane.setHgrow(descriptionField, Priority.ALWAYS);

        Button add = new Button("Add");
        Button clear = new Button("Clear form");
        add.getStyleClass().add("primary-button");
        deleteButton.getStyleClass().add("danger-button");
        add.setOnAction(e -> onAddTransaction());
        updateButton.setOnAction(e -> onUpdateTransaction());
        deleteButton.setOnAction(e -> onDeleteTransaction());
        clear.setOnAction(e -> clearTransactionForm());
        updateButton.setDisable(true);
        deleteButton.setDisable(true);

        HBox buttons = new HBox(10, add, updateButton, deleteButton, clear, spacer(), allMonths);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox formCard = card(form, buttons);

        transactionTable.getColumns().add(column("ID", Transaction::getTransactionId, 90));
        transactionTable.getColumns().add(column("Date", t -> t.getDate().toString(), 110));
        transactionTable.getColumns().add(column("Type", t -> capitalise(t.getTransactionType().name().toLowerCase()), 100));
        transactionTable.getColumns().add(column("Description", Transaction::getDescription, 300));
        transactionTable.getColumns().add(numericColumn("Amount (RM)", t -> String.format("%,.2f", t.getAmount()), 120));
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        transactionTable.setPlaceholder(new Label("No transactions to show."));

        // picking a row loads it into the form so you can update or delete it
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, old, t) -> {
            boolean hasSelection = t != null;
            updateButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            if (hasSelection) {
                typeBox.setValue(t.getTransactionType());
                datePicker.setValue(t.getDate());
                descriptionField.setText(t.getDescription());
                amountField.setText(String.valueOf(t.getAmount()));
            }
        });

        VBox box = new VBox(16, formCard, transactionTable);
        box.setPadding(new Insets(20, 28, 24, 28));
        VBox.setVgrow(transactionTable, Priority.ALWAYS);
        return box;
    }

    private record Entry(LocalDate date, double amount, String description, TransactionType type) {
    }

    // reads and validates the form; shows an error and returns null if invalid
    private Entry readForm() {
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            showError("Description cannot be empty.");
            return null;
        }
        Double amount = parseAmount(amountField.getText());
        if (amount == null) {
            return null;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Please pick a date.");
            return null;
        }
        return new Entry(date, amount, description, typeBox.getValue());
    }

    private void onAddTransaction() {
        Entry entry = readForm();
        if (entry == null) {
            return;
        }
        String id = newId();
        run(() -> {
            if (transactionController.addTransaction(id, entry.date(), entry.amount(),
                    entry.description(), entry.type())) {
                currentMonth = YearMonth.from(entry.date());   // so the new row is visible
                descriptionField.clear();
                amountField.clear();
                refreshAll();
                descriptionField.requestFocus();               // ready for the next one
            } else {
                showError("Could not add the transaction.");
            }
        });
    }

    private void onUpdateTransaction() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Entry entry = readForm();
        if (entry == null) {
            return;
        }
        run(() -> {
            if (transactionController.updateTransaction(selected.getTransactionId(), entry.date(),
                    entry.amount(), entry.description(), entry.type())) {
                clearTransactionForm();
                refreshAll();
            } else {
                showError("Could not update the transaction.");
            }
        });
    }

    private void onDeleteTransaction() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getDescription() + "\" (" + money(selected.getAmount()) + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        applyStyle(confirm);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        run(() -> {
            if (transactionController.deleteTransaction(selected.getTransactionId())) {
                clearTransactionForm();
                refreshAll();
            } else {
                showError("Could not delete the transaction.");
            }
        });
    }

    private void clearTransactionForm() {
        transactionTable.getSelectionModel().clearSelection();
        typeBox.setValue(TransactionType.EXPENSE);
        datePicker.setValue(LocalDate.now());
        descriptionField.clear();
        amountField.clear();
    }

    private void refreshTransactions() {
        List<Transaction> source = allMonths.isSelected()
                ? transactionController.getAllTransactions()
                : transactionController.getMonthlyTransaction(currentMonth);

        // newest first; sorting a copy so the service's own list keeps its order
        List<Transaction> sorted = source.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .toList();
        transactionRows.setAll(sorted);
    }

    // =====================================================================
    // budgets tab
    // =====================================================================

    private VBox buildBudgetsTab() {
        budgetTitle.getStyleClass().add("section-title");
        budgetCategoryField.setPromptText("Category, e.g. Food");
        budgetLimitField.setPromptText("Limit (RM)");
        budgetLimitField.setOnAction(e -> onSetBudget());

        Button set = new Button("Set budget");
        Button clear = new Button("Clear form");
        set.getStyleClass().add("primary-button");
        set.setOnAction(e -> onSetBudget());
        clear.setOnAction(e -> {
            budgetTable.getSelectionModel().clearSelection();
            budgetCategoryField.clear();
            budgetLimitField.clear();
        });

        HBox form = new HBox(10, budgetCategoryField, budgetLimitField, set, clear);
        HBox.setHgrow(budgetCategoryField, Priority.ALWAYS);

        Label hint = new Label("The category must match the transaction description (e.g. Food). "
                + "Setting a budget for an existing category replaces its limit.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        VBox formCard = card(budgetTitle, form, hint);

        budgetTable.getColumns().add(column("Category", BudgetRow::category, 220));
        budgetTable.getColumns().add(numericColumn("Limit (RM)", r -> String.format("%,.2f", r.limit()), 110));
        budgetTable.getColumns().add(numericColumn("Spent (RM)", r -> String.format("%,.2f", r.spent()), 110));
        budgetTable.getColumns().add(numericColumn("Remaining (RM)", r -> String.format("%,.2f", r.remaining()), 130));
        budgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        budgetTable.setPlaceholder(new Label("No budgets for this month."));

        // over-budget rows get the "over-budget" style class (red text in finance.css)
        budgetTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BudgetRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("over-budget");
                if (!empty && item != null && item.remaining() < 0) {
                    getStyleClass().add("over-budget");
                }
            }
        });

        // picking a row loads it into the form so you can change its limit
        budgetTable.getSelectionModel().selectedItemProperty().addListener((obs, old, r) -> {
            if (r != null) {
                budgetCategoryField.setText(r.category());
                budgetLimitField.setText(String.valueOf(r.limit()));
            }
        });

        VBox box = new VBox(16, formCard, budgetTable);
        box.setPadding(new Insets(20, 28, 24, 28));
        VBox.setVgrow(budgetTable, Priority.ALWAYS);
        return box;
    }

    private void onSetBudget() {
        String category = budgetCategoryField.getText().trim();
        if (category.isEmpty()) {
            showError("Category cannot be empty.");
            return;
        }
        Double limit = parseAmount(budgetLimitField.getText());
        if (limit == null) {
            return;
        }
        run(() -> {
            if (budgetController.setBudget(currentMonth, category, limit)) {
                budgetCategoryField.clear();
                budgetLimitField.clear();
                budgetTable.getSelectionModel().clearSelection();
                refreshAll();
            } else {
                showError("Could not save the budget.");
            }
        });
    }

    private void refreshBudgets() {
        budgetTitle.setText("Budgets for " + currentMonth);

        // spent = this month's expenses whose description matches the category
        List<Transaction> monthExpenses = transactionController.getMonthlyTransaction(currentMonth).stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .toList();

        List<BudgetRow> rows = new ArrayList<>();
        for (Budget b : budgetController.getBudgets(currentMonth)) {
            double spent = monthExpenses.stream()
                    .filter(t -> t.getDescription().equalsIgnoreCase(b.getCategory()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();
            rows.add(new BudgetRow(b.getCategory(), b.getLimit(), spent));
        }
        rows.sort(Comparator.comparing(BudgetRow::category, String.CASE_INSENSITIVE_ORDER));
        budgetRows.setAll(rows);
    }

    // =====================================================================
    // summary tab
    // =====================================================================

    private VBox buildSummaryTab() {
        incomeLabel.getStyleClass().add("positive");
        expenseLabel.getStyleClass().add("negative");

        HBox stats = new HBox(16,
                statCard("Income", incomeLabel),
                statCard("Expenses", expenseLabel),
                statCard("Balance", balanceLabel));

        expenseChart.setTitle("Expenses by category");
        expenseChart.setLegendSide(Side.RIGHT);
        expenseChart.setLabelsVisible(false);
        VBox.setVgrow(expenseChart, Priority.ALWAYS);

        VBox chartCard = card(expenseChart);
        VBox.setVgrow(chartCard, Priority.ALWAYS);

        VBox box = new VBox(16, stats, chartCard);
        box.setPadding(new Insets(20, 28, 24, 28));
        return box;
    }

    private void refreshSummary() {
        double income = transactionController.getMonthlyTotal(currentMonth, TransactionType.INCOME);
        double expense = transactionController.getMonthlyTotal(currentMonth, TransactionType.EXPENSE);
        double balance = income - expense;

        incomeLabel.setText(money(income));
        expenseLabel.setText(money(expense));
        balanceLabel.setText(money(balance));
        balanceLabel.getStyleClass().removeAll("positive", "negative");
        balanceLabel.getStyleClass().add(balance < 0 ? "negative" : "positive");

        // expenses grouped by description, ignoring upper/lower case
        Map<String, Double> byCategory = transactionController.getMonthlyTransaction(currentMonth).stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getDescription().trim().toLowerCase(),
                        TreeMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));

        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        byCategory.forEach((name, total) ->
                slices.add(new PieChart.Data(capitalise(name) + "  " + money(total), total)));
        expenseChart.setData(slices);
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private void refreshAll() {
        monthLabel.setText(currentMonth.toString());
        refreshTransactions();
        refreshBudgets();
        refreshSummary();
    }

    // runs an action and shows any unexpected error (e.g. a failed save) instead of crashing
    private void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            showError("Something went wrong: " + e.getMessage());
        }
    }

    // ---------- small UI builders ----------

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static VBox card(Node... content) {
        VBox card = new VBox(14, content);
        card.getStyleClass().add("card");
        return card;
    }

    private static VBox statCard(String title, Label value) {
        value.getStyleClass().add("stat-value");
        VBox card = card(captionLabel(title), value);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Label captionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, String> value, double width) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    // same as column(), but the numbers are right-aligned ("numeric" style class)
    private static <T> TableColumn<T, String> numericColumn(String title, Function<T, String> value, double width) {
        TableColumn<T, String> col = column(title, value, width);
        col.getStyleClass().add("numeric");
        return col;
    }

    // ---------- input and dialogs ----------

    // returns null (after showing an error) when the text is not a positive number
    private Double parseAmount(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            if (Double.isFinite(value) && value > 0) {
                return value;
            }
            showError("Amount must be greater than 0.");
        } catch (NumberFormatException e) {
            showError("Please enter a valid number, e.g. 12.50");
        }
        return null;
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        applyStyle(alert);
        alert.showAndWait();
    }

    // dialogs are separate windows, so they need the stylesheet too
    private static void applyStyle(Alert alert) {
        if (stylesheetUrl != null) {
            alert.getDialogPane().getStylesheets().add(stylesheetUrl);
        }
    }

    // ---------- formatting ----------

    private static String money(double value) {
        return String.format("RM %,.2f", value);
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    // a short unique ID such as "3F9A1C", checked against existing IDs (same idea as the console View)
    private String newId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (transactionController.findById(id).isPresent());
        return id;
    }
}
