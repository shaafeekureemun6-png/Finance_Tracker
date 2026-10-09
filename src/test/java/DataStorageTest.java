import com.shaafee.TransactionStorage.DataStorage;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataStorageTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private DataStorage storage;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("finance.db");
        storage = new DataStorage(dbFile);
    }

    // ---------- empty database ----------

    @Test
    void load_emptyDatabase_returnsEmptyLists() {
        assertTrue(storage.loadTransactions().isEmpty());
        assertTrue(storage.loadBudgets().isEmpty());
    }

    @Test
    void saveEmptyLists_thenLoad_returnsEmptyLists() {
        storage.save(List.of(), List.of());

        assertTrue(Files.exists(dbFile));
        assertTrue(new DataStorage(dbFile).loadTransactions().isEmpty());
        assertTrue(new DataStorage(dbFile).loadBudgets().isEmpty());
    }

    // ---------- save, then load back ----------

    @Test
    void saveThenLoad_transactionsComeBackIdentical() {
        Transaction expense = new Transaction("T1", LocalDate.of(2026, 10, 1), 12.50, "Food", TransactionType.EXPENSE);
        Transaction income = new Transaction("T2", LocalDate.of(2026, 10, 3), 800.00, "Salary", TransactionType.INCOME);

        storage.save(List.of(expense, income), List.of());

        // A new DataStorage instance proves data was persisted to the SQLite file
        List<Transaction> loaded = new DataStorage(dbFile).loadTransactions();

        assertEquals(2, loaded.size());

        assertEquals("T1", loaded.get(0).getTransactionId());
        assertEquals(LocalDate.of(2026, 10, 1), loaded.get(0).getDate());
        assertEquals(12.50, loaded.get(0).getAmount(), 0.001);
        assertEquals("Food", loaded.get(0).getDescription());
        assertEquals(TransactionType.EXPENSE, loaded.get(0).getTransactionType());

        assertEquals("T2", loaded.get(1).getTransactionId());
        assertEquals(800.00, loaded.get(1).getAmount(), 0.001);
        assertEquals(TransactionType.INCOME, loaded.get(1).getTransactionType());
    }

    @Test
    void saveThenLoad_budgetsComeBackIdentical() {
        Budget food = new Budget(YearMonth.of(2026, 10), "Food", 500.00);
        Budget transport = new Budget(YearMonth.of(2026, 11), "Transport", 150.50);

        storage.save(List.of(), List.of(food, transport));

        List<Budget> loaded = new DataStorage(dbFile).loadBudgets();

        assertEquals(2, loaded.size());
        assertEquals(YearMonth.of(2026, 10), loaded.get(0).getMonth());
        assertEquals("Food", loaded.get(0).getCategory());
        assertEquals(500.00, loaded.get(0).getLimit(), 0.001);
        assertEquals(YearMonth.of(2026, 11), loaded.get(1).getMonth());
        assertEquals(150.50, loaded.get(1).getLimit(), 0.001);
    }

    // ---------- file & directory creation ----------

    @Test
    void save_createsMissingFolders() {
        Path nested = tempDir.resolve("data").resolve("finance.db");
        DataStorage nestedStorage = new DataStorage(nested);

        nestedStorage.save(List.of(), List.of());

        assertTrue(Files.exists(nested));
    }

    @Test
    void save_overwritesPreviousContentInDatabase() {
        Transaction t1 = new Transaction("T1", LocalDate.of(2026, 10, 1), 1.0, "Food", TransactionType.EXPENSE);
        Transaction t2 = new Transaction("T2", LocalDate.of(2026, 10, 2), 2.0, "Food", TransactionType.EXPENSE);

        storage.save(List.of(t1, t2), List.of());
        storage.save(List.of(t2), List.of());

        List<Transaction> loaded = new DataStorage(dbFile).loadTransactions();
        assertEquals(1, loaded.size());
        assertEquals("T2", loaded.get(0).getTransactionId());
    }

    // ---------- text and special character safety ----------

    @Test
    void save_commasAndQuotesInDescriptionPreservedInDatabase() {
        Transaction t = new Transaction("T1", LocalDate.of(2026, 10, 1), 12.50, "Lunch, drinks & 'snacks'", TransactionType.EXPENSE);

        storage.save(List.of(t), List.of());

        List<Transaction> loaded = new DataStorage(dbFile).loadTransactions();
        assertEquals(1, loaded.size());
        assertEquals("Lunch, drinks & 'snacks'", loaded.get(0).getDescription());
        assertEquals(12.50, loaded.get(0).getAmount(), 0.001);
    }

    @Test
    void save_lineBreaksInDescriptionPreservedInDatabase() {
        Transaction t = new Transaction("T1", LocalDate.of(2026, 10, 1), 5.0, "Line one\nLine two", TransactionType.EXPENSE);

        storage.save(List.of(t), List.of());

        List<Transaction> loaded = new DataStorage(dbFile).loadTransactions();
        assertEquals(1, loaded.size());
        assertEquals("Line one\nLine two", loaded.get(0).getDescription());
    }
}

