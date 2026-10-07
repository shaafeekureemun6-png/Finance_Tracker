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

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {

        // 1. Create the storage (the file is created on the first save)
        DataStorage storage = new DataStorage(Path.of("data", "finance.csv"));

        // 2. Create the services (they hold the data in memory)
        TransactionService transactionService = new TransactionService();
        BudgetService budgetService = new BudgetService();

        // 3. Load saved data from the file into the services
        try {
            for (Transaction t : storage.loadTransactions()) {
                transactionService.addTransaction(t);
            }
            for (Budget b : storage.loadBudgets()) {
                budgetService.setBudget(b);
            }
        } catch (UncheckedIOException e) {
            // Stop here: if we carried on, the first save would overwrite
            // the unreadable file with an empty one.
            System.out.println("Could not read the data file: " + e.getMessage());
            return;
        }

        // 4. What the controllers run after every successful change
        Runnable saveAll = () ->
                storage.save(transactionService.getTransactions(), budgetService.getBudgets());

        // 5. Create the controllers, sharing the SAME service objects
        TransactionController transactionController =
                new TransactionController(transactionService, saveAll);
        BudgetController budgetController =
                new BudgetController(budgetService, transactionService, saveAll);

        // 6. Start the console menu
        new View(transactionController, budgetController).run();

    }
}
