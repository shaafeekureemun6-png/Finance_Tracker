package com.shaafee;

import com.shaafee.TransactionStorage.DataStorage;
import com.shaafee.UserInterface.View;
import com.shaafee.controller.BudgetController;
import com.shaafee.controller.TransactionController;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.service.BudgetService;
import com.shaafee.service.TransactionService;

import java.io.UncheckedIOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        run(Path.of("data", "finance.db"));
    }

    // Package-private on purpose: tests call this with a temporary file,
    // so they never touch your real data/finance.csv.
    public static void run(Path dataFile) {

        DataStorage storage = new DataStorage(dataFile);

        TransactionService transactionService = new TransactionService();
        BudgetService budgetService = new BudgetService();

        // CHANGED: Catch RuntimeException instead of UncheckedIOException
        try {
            for (Transaction t : storage.loadTransactions()) {
                transactionService.addTransaction(t);
            }
            for (Budget b : storage.loadBudgets()) {
                budgetService.setBudget(b);
            }
        } catch (RuntimeException e) {
            System.out.println("Could not read the database: " + e.getMessage());
            return;
        }

        Runnable saveAll = () ->
                storage.save(transactionService.getTransactions(), budgetService.getBudgets());

        TransactionController transactionController =
                new TransactionController(transactionService, saveAll);
        BudgetController budgetController =
                new BudgetController(budgetService, transactionService, saveAll);

        new View(transactionController, budgetController).run();
    }
}