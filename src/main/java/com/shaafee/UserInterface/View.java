package com.shaafee.UserInterface;

import com.shaafee.controller.BudgetController;
import com.shaafee.controller.TransactionController;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class View {

    private final TransactionController transactionController;
    private final BudgetController budgetController;
    private final Scanner scanner = new Scanner(System.in);

    public View(TransactionController transactionController, BudgetController budgetController) {
        this.transactionController = transactionController;
        this.budgetController = budgetController;
    }

    // ====================== main menu loop ======================

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                break;   // input was closed
            }
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addTransaction();
                    case "2" -> printTransactions(transactionController.getAllTransactions());
                    case "3" -> viewByMonth();
                    case "4" -> editTransaction();
                    case "5" -> deleteTransaction();
                    case "6" -> monthlySummary();
                    case "7" -> categoryTotal();
                    case "8" -> setBudget();
                    case "0" -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice. Please enter a number from 0 to 9.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("===== Personal Finance Tracker =====");
        System.out.println("1. Add transaction");
        System.out.println("2. View all transactions");
        System.out.println("3. View transactions by month");
        System.out.println("4. Edit transaction");
        System.out.println("5. Delete transaction");
        System.out.println("6. Monthly summary");
        System.out.println("7. Total by category");
        System.out.println("8. Set monthly budget");
        System.out.println("9. View budget vs spent");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    // ====================== transactions ======================

    private void addTransaction() {
        System.out.println("--- Add transaction ---");
        TransactionType type = readType(null);
        String description = readText("Description (also its category, e.g. Food): ", null);
        double amount = readAmount("Amount (RM): ", null);
        LocalDate date = readDate("Date yyyy-MM-dd (Enter = today): ", LocalDate.now());
        String id = newId();

        if (transactionController.addTransaction(id, date, amount, description, type)) {
            System.out.println("Transaction added. ID: " + id);

        } else {
            System.out.println("Could not add the transaction.");
        }
    }

    private void viewByMonth() {
        YearMonth month = readMonth("Month yyyy-MM (Enter = this month): ", YearMonth.now());
        System.out.println("Transactions for " + month + ":");
        printTransactions(transactionController.getMonthlyTransaction(month));
    }

    private void editTransaction() {
        System.out.println("--- Edit transaction ---");
        String id = readText("Enter the transaction ID to edit: ", null).toUpperCase();

        Optional<Transaction> found = transactionController.findById(id);
        if (found.isEmpty()) {
            System.out.println("No transaction with ID " + id);
            return;
        }
        Transaction current = found.get();

        System.out.println("Press Enter to keep the current value.");
        TransactionType type = readType(current.getTransactionType());
        String description = readText("Description [" + current.getDescription() + "]: ",
                current.getDescription());
        double amount = readAmount("Amount [" + current.getAmount() + "]: ", current.getAmount());
        LocalDate date = readDate("Date [" + current.getDate() + "]: ", current.getDate());

        if (transactionController.updateTransaction(id, date, amount, description, type)) {
            System.out.println("Transaction updated.");
        } else {
            System.out.println("Could not update the transaction.");
        }
    }

    private void deleteTransaction() {
        System.out.println("--- Delete transaction ---");
        String id = readText("Enter the transaction ID to delete: ", null).toUpperCase();

        Optional<Transaction> found = transactionController.findById(id);
        if (found.isEmpty()) {
            System.out.println("No transaction with ID " + id);
            return;
        }

        printTransactions(List.of(found.get()));
        if (!confirm("Delete this transaction?")) {
            System.out.println("Cancelled.");
            return;
        }

        if (transactionController.deleteTransaction(id)) {
            System.out.println("Transaction deleted.");
        } else {
            System.out.println("Could not delete the transaction.");
        }
    }

    // ====================== totals ======================

    private void monthlySummary() {
        YearMonth month = readMonth("Month yyyy-MM (Enter = this month): ", YearMonth.now());
        double income = transactionController.getMonthlyTotal(month, TransactionType.INCOME);
        double expense = transactionController.getMonthlyTotal(month, TransactionType.EXPENSE);

        System.out.println("Summary for " + month);
        System.out.printf("Income:  RM%.2f%n", income);
        System.out.printf("Expense: RM%.2f%n", expense);
        System.out.printf("Balance: RM%.2f%n", income - expense);
    }

    private void categoryTotal() {
        String category = readText("Category / description (e.g. Food): ", null);
        TransactionType type = readType(null);
        double total = transactionController.getCategoryTotal(category, type);

        System.out.printf("%s total for \"%s\" (all months): RM%.2f%n", type, category, total);
    }

    // ====================== budgets ======================

    private void setBudget() {
        System.out.println("--- Set monthly budget ---");
        YearMonth month = readMonth("Month yyyy-MM (Enter = this month): ", YearMonth.now());
        String category = readText("Category (must match the transaction description, e.g. Food): ", null);
        double limit = readAmount("Budget limit (RM): ", null);

        if (budgetController.setBudget(month, category, limit)) {
            System.out.printf("Budget saved: %s, %s, RM%.2f%n", month, category, limit);
        } else {
            System.out.println("Could not save the budget.");
        }
    }


    // ====================== printing ======================

    private void printTransactions(List<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        System.out.printf("%-8s %-12s %-8s %-20s %10s%n",
                "ID", "Date", "Type", "Description", "Amount");
        for (Transaction t : list) {
            System.out.printf("%-8s %-12s %-8s %-20s %10.2f%n",
                    t.getTransactionId(), t.getDate(), t.getTransactionType(),
                    t.getDescription(), t.getAmount());
        }
    }

    // ====================== input helpers ======================
    // For each reader, a null default means "this value is required".

    private String readText(String prompt, String defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            if (defaultValue != null) {
                return defaultValue;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private double readAmount(String prompt, Double defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                double value = Double.parseDouble(input);
                if (Double.isFinite(value) && value > 0) {
                    return value;
                }
                System.out.println("Amount must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number, e.g. 12.50");
            }
        }
    }

    private LocalDate readDate(String prompt, LocalDate defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-MM-dd, e.g. 2026-10-01");
            }
        }
    }

    private YearMonth readMonth(String prompt, YearMonth defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid month. Use yyyy-MM, e.g. 2026-10");
            }
        }
    }

    private TransactionType readType(TransactionType defaultValue) {
        String hint = (defaultValue == null) ? "" : " [" + defaultValue + "]";
        while (true) {
            System.out.print("Type (1 = Income, 2 = Expense)" + hint + ": ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (input.equals("1")) {
                return TransactionType.INCOME;
            }
            if (input.equals("2")) {
                return TransactionType.EXPENSE;
            }
            System.out.println("Please enter 1 or 2.");
        }
    }

    private boolean confirm(String question) {
        System.out.print(question + " (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }

    // a short unique ID such as "3F9A1C", checked against existing IDs
    private String newId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (transactionController.findById(id).isPresent());
        return id;
    }
}