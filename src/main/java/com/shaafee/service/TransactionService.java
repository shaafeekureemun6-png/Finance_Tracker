package com.shaafee.service;

import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionService {
    // to store new transactions while app runs
   private final List<Transaction> transactions=new ArrayList<Transaction>();

   //add to transaction
    public boolean addTransaction(Transaction transaction){
        transactions.add(transaction);
        return true;
    }
    //delete transaction
    public boolean deleteTransaction(String transactionId){
        if(transactions.removeIf(transaction->transaction.getTransactionId().equals(transactionId)))
        return true;
        return false;
    }
    //edit transaction
    public boolean updateTransaction(String transactionId, Transaction transactionb){
        if(transactions.stream().anyMatch(transaction->transaction.getTransactionId().equals(transactionId))){
            transactions.removeIf(transaction->transaction.getTransactionId().equals(transactionId));
            transactions.add(transactionb);
            return true;
        }
        return false;
    }
    //get all transactions
    public List<Transaction> getTransactions(){
        return transactions;
    }
    // get monthly transactions
    public List<Transaction> getMonthlyTransaction(YearMonth Month){
        return transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(Month)).
                collect(Collectors.toList());
    }

    //get sum of all incomes and expenses  for a month
    public double getMonthlytotal(YearMonth Month, TransactionType type){
        return transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(Month)).
                filter(t ->  t.getTransactionType() == type).
                mapToDouble(Transaction::getAmount).sum();

    }
    //get sum of categorized incomes and expenses
    public double getCategoryTotal(String category, TransactionType type){
        return transactions.stream().filter(t->t.getDescription().equals(category)).
                filter(transaction -> transaction.getTransactionType()==type).
                mapToDouble(Transaction::getAmount).sum();
    }

}
