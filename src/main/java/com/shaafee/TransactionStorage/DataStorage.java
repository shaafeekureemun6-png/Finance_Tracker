package com.shaafee.TransactionStorage;

import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {

    private final String dbUrl;

    public DataStorage(Path file) {
        this.dbUrl = "jdbc:sqlite:" + file.toAbsolutePath().toString();

        // Ensure parent directory ("data/") exists before creating the DB file
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory for database", e);
        }

        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initializeDatabase() {
        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions ("
                + "id TEXT PRIMARY KEY, "
                + "date TEXT NOT NULL, "
                + "type TEXT NOT NULL, "
                + "description TEXT, "
                + "amount REAL NOT NULL"
                + ");";

        String createBudgetsTable = "CREATE TABLE IF NOT EXISTS budgets ("
                + "month TEXT NOT NULL, "
                + "category TEXT NOT NULL, "
                + "limit_amount REAL NOT NULL, "
                + "PRIMARY KEY (month, category)"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactionsTable);
            stmt.execute(createBudgetsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize SQLite database", e);
        }
    }

    public List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<>();
        String sql = "SELECT id, date, type, description, amount FROM transactions";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new Transaction(
                        rs.getString("id"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        TransactionType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading transactions from database: " + e.getMessage());
        }
        return result;
    }

    public List<Budget> loadBudgets() {
        List<Budget> result = new ArrayList<>();
        String sql = "SELECT month, category, limit_amount FROM budgets";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new Budget(
                        YearMonth.parse(rs.getString("month")),
                        rs.getString("category"),
                        rs.getDouble("limit_amount")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading budgets from database: " + e.getMessage());
        }
        return result;
    }

    public void save(List<Transaction> transactions, List<Budget> budgets) {
        String clearTxns = "DELETE FROM transactions";
        String insertTxn = "INSERT INTO transactions (id, date, type, description, amount) VALUES (?, ?, ?, ?, ?)";

        String clearBudgets = "DELETE FROM budgets";
        String insertBudget = "INSERT INTO budgets (month, category, limit_amount) VALUES (?, ?, ?)";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement();
                 PreparedStatement pstmt = conn.prepareStatement(insertTxn)) {
                stmt.executeUpdate(clearTxns);
                for (Transaction t : transactions) {
                    pstmt.setString(1, t.getTransactionId());
                    pstmt.setString(2, t.getDate().toString());
                    pstmt.setString(3, t.getTransactionType().name());
                    pstmt.setString(4, t.getDescription());
                    pstmt.setDouble(5, t.getAmount());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            try (Statement stmt = conn.createStatement();
                 PreparedStatement pstmt = conn.prepareStatement(insertBudget)) {
                stmt.executeUpdate(clearBudgets);
                for (Budget b : budgets) {
                    pstmt.setString(1, b.getMonth().toString());
                    pstmt.setString(2, b.getCategory());
                    pstmt.setDouble(3, b.getLimit());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save data to SQLite database", e);
        }
    }
}






