package com.shaafee.TransactionStorage;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;



public class DataStorage {

    private static final String TXN ="TXN";
    private static final String Budget="BUDGET";

    private final Path file;

    public DataStorage(Path file) {
        this.file = file;
    }
    //reading lines from files
    public List<String> readLines() {

        if(!Files.exists(file)) {
            return new ArrayList<>();
        }
        try{
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read data from " + file, e);
        }

    }
    // cleaning string
    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(",", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    //loading from file to list
    public List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<>();
        for (String line : readLines()) {
            String[] p = line.split(",");
            if (!p[0].equals(TXN)) {
                continue;
            }
            try {
                // TXN,id,date,type,description,amount
                result.add(new Transaction(
                        p[1],                              // id
                        LocalDate.parse(p[2]),             // date
                        Double.parseDouble(p[5]),          // amount
                        p[4],                              // description
                        TransactionType.valueOf(p[3])));   // type
            } catch (RuntimeException e) {
                // bad number, bad date, unknown type, missing column...
                System.out.println("Skipping unreadable line: " + line);
            }
        }
        return result;
    }
    // loading budgets
    public List<Budget> loadBudgets() {
        List<Budget> result = new ArrayList<>();
        for (String line : readLines()) {
            String[] p= line.split(",");
            if (!p[0].equals(Budget)) {
                continue;
            }
            try {
                result.add(new Budget(
                        YearMonth.parse(p[1]),
                        p[2],
                        Double.parseDouble(p[3])));
            } catch (RuntimeException e) {
                System.out.println("Skipping unreadable line: " + line);
            }
        }
        return result;


    }
    // saving transactions and budgets
    public void save(List<Transaction> transactions, List<Budget> budgets) {
        List<String> lines = new ArrayList<>();

        for (Transaction t : transactions) {
            lines.add(String.join(",",
                    TXN,
                    clean(t.getTransactionId()),
                    t.getDate().toString(),
                    t.getTransactionType().name(),
                    clean(t.getDescription()),
                    String.valueOf(t.getAmount())));
        }

        for (Budget b : budgets) {
            lines.add(String.join(",",
                    Budget,
                    b.getMonth().toString(),
                    clean(b.getCategory()),
                    String.valueOf(b.getLimit())));
        }

        try {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);

            // Write to a temporary file first, then swap it in.
            // If the program crashes mid-write, the real file stays intact.
            Path temp = folder.resolve(file.getFileName() + ".tmp");
            Files.write(temp, lines);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save data to " + file, e);
        }
    }



}




