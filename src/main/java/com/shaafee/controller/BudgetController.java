package com.shaafee.controller;

import com.shaafee.model.Budget;
import com.shaafee.model.TransactionType;
import com.shaafee.service.BudgetService;
import com.shaafee.service.TransactionService;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetController {

    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private final Runnable saveAction;   // runs after every successful change

    public BudgetController(BudgetService budgetService,
                            TransactionService transactionService,
                            Runnable saveAction) {
        this.budgetService = budgetService;
        this.transactionService = transactionService;
        this.saveAction = saveAction;
    }

    // set a budget for a month and category (category = transaction description)
    public boolean setBudget(YearMonth month, String category, double limit) {
        if (month == null || category == null || category.isBlank() || limit <= 0) {
            return false;
        }
        boolean saved = budgetService.setBudget(new Budget(month, category.trim(), limit));
        if (saved) {
            saveAction.run();
        }
        return saved;
    }

    // all budgets that belong to one month
    public List<Budget> getBudgets(YearMonth month) {
        if (month == null) {
            return List.of();
        }
        return budgetService.getBudgets().stream()
                .filter(b -> b.getMonth().equals(month))
                .collect(Collectors.toList());
    }


}