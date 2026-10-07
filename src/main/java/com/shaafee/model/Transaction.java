package com.shaafee.model;

import java.time.LocalDate;
import java.util.Date;

public class Transaction {
    private String transactionId;
    private LocalDate date;
    private double amount;
    private String description;
    private TransactionType transactionType;

    public Transaction(String transactionId, LocalDate date, double amount, String description, TransactionType transactionType) {
        this.transactionId = transactionId;
        this.date = date;
        this.amount = amount;
        this.description = description;
        this.transactionType = transactionType;

    }
    public Transaction() {}

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String
    toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", date=" + date +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                '}';
    }
}
