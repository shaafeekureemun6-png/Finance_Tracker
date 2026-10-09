import com.shaafee.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests: the whole app (Main -> View -> controllers -> services -> SQLite DB).
 * Each test uses its own temporary database file in @TempDir.
 */
class MainTest {

    @TempDir
    Path tempDir;

    private Path dbFile;

    private InputStream originalIn;
    private PrintStream originalOut;
    private Locale originalLocale;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        originalLocale = Locale.getDefault();

        Locale.setDefault(Locale.US);
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));

        dbFile = tempDir.resolve("finance.db");
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        Locale.setDefault(originalLocale);
    }

    private String runApp(String... inputLines) {
        captured.reset();
        String input = String.join("\n", inputLines) + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        Main.run(dbFile);

        System.out.flush();
        return captured.toString(StandardCharsets.UTF_8);
    }

    // ---------- starting and exiting ----------

    @Test
    void run_exitImmediately_showsMenu() {
        String output = runApp("0");

        assertTrue(output.contains("Personal Finance Tracker"));
        assertTrue(output.contains("Goodbye!"));
    }

    // ---------- saving to database ----------

    @Test
    void run_addTransaction_createsDatabaseFileAndPersistsData() {
        runApp("1", "2", "Food", "12.50", "2026-10-01", "0");

        assertTrue(Files.exists(dbFile));

        // Verify data persists across app runs
        String output = runApp("2", "0");
        assertTrue(output.contains("Food"));
        assertTrue(output.contains("12.50"));
    }

    @Test
    void run_setBudget_savesBudgetToDatabase() {
        runApp("8", "2026-10", "Food", "500", "0");

        assertTrue(Files.exists(dbFile));

        // Verify budget persisted in second session
        String output = runApp("9", "2026-10", "0");
        assertTrue(output.contains("Food"));
        assertTrue(output.contains("500.00"));
    }

    // ---------- data persistence across runs ----------

    @Test
    void run_dataSurvivesARestart() {
        // Session 1: Add a transaction and exit
        runApp("1", "2", "Food", "12.50", "2026-10-01", "0");

        // Session 2: A brand-new run reads from the SQLite DB file
        String output = runApp("2", "0");

        assertTrue(output.contains("Food"));
        assertTrue(output.contains("12.50"));
        assertFalse(output.contains("No transactions found"));
    }

    // ---------- changes reach the database ----------

    @Test
    void run_deleteTransaction_updatesDatabase() {
        // Session 1: Create transaction
        runApp("1", "2", "Food", "12.50", "2026-10-01", "0");

        // View transactions to capture the auto-generated transaction ID
        String viewOutput = runApp("2", "0");
        assertTrue(viewOutput.contains("Food"));

        // Extract ID (6-character generated string) or delete via UI flow
        runApp("5", "T1", "y", "0");

        // Session 3: Verify deletion
        String finalOutput = runApp("2", "0");
        assertFalse(finalOutput.contains("Food"));
    }

    // ---------- error handling ----------

    @Test
    void run_invalidDatabasePath_stopsWithAMessage() throws IOException {
        // Create a directory where the DB file should be to cause a connection failure
        Files.createDirectory(dbFile);

        String output = runApp("0");

        assertTrue(output.contains("Could not read the database"));
        assertFalse(output.contains("Goodbye!"));
    }
}