package com.shaafee.controller;

import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.TransactionService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class TransactionController {

    private final TransactionService transactionService;
    private final Runnable saveAction;   // runs after every successful change

    public TransactionController(TransactionService transactionService, Runnable saveAction) {
        this.transactionService = transactionService;
        this.saveAction = saveAction;
    }

    // ---------- add / update / delete (return true if it worked) ----------

    public boolean addTransaction(String transactionId, LocalDate date, double amount,
                                  String description, TransactionType transactionType) {
        if (!isValid(transactionId, date, amount, description, transactionType)) {
            return false;
        }
        boolean added = transactionService.addTransaction(
                new Transaction(transactionId, date, amount, description.trim(), transactionType));
        if (added) {
            saveAction.run();
        }
        return added;
    }

    public boolean updateTransaction(String transactionId, LocalDate date, double amount,
                                     String description, TransactionType transactionType) {
        if (!isValid(transactionId, date, amount, description, transactionType)) {
            return false;
        }
        boolean updated = transactionService.updateTransaction(transactionId,
                new Transaction(transactionId, date, amount, description.trim(), transactionType));
        if (updated) {
            saveAction.run();
        }
        return updated;
    }

    public boolean deleteTransaction(String transactionId) {
        boolean deleted = transactionService.deleteTransaction(transactionId);
        if (deleted) {
            saveAction.run();
        }
        return deleted;
    }

    // ---------- reading ----------

    public List<Transaction> getAllTransactions() {
        return transactionService.getTransactions();
    }

    public Optional<Transaction> findById(String transactionId) {
        return transactionService.getTransactions().stream()
                .filter(t -> t.getTransactionId().equals(transactionId))
                .findFirst();
    }

    public List<Transaction> getMonthlyTransaction(YearMonth month) {
        if (month == null) {
            return List.of();
        }
        return transactionService.getMonthlyTransaction(month);
    }

    // ---------- totals ----------

    public double getMonthlyTotal(YearMonth month, TransactionType type) {
        if (month == null || type == null) {
            return 0;
        }
        return transactionService.getMonthlytotal(month, type);
    }

    public double getCategoryTotal(String category, TransactionType type) {
        if (category == null || type == null) {
            return 0;
        }
        return transactionService.getCategoryTotal(category, type);
    }

    // ---------- helper ----------

    private boolean isValid(String transactionId, LocalDate date, double amount,
                            String description, TransactionType type) {
        return transactionId != null && !transactionId.isBlank()
                && date != null
                && amount > 0
                && description != null && !description.isBlank()
                && type != null;
    }
}