import com.shaafee.UserInterface.View;
import com.shaafee.controller.BudgetController;
import com.shaafee.controller.TransactionController;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.BudgetService;
import com.shaafee.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**




 */
class ViewTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);
    private static final LocalDate NOV_2 = LocalDate.of(2026, 11, 2);

    private TransactionService transactionService;
    private BudgetService budgetService;
    private TransactionController transactionController;
    private BudgetController budgetController;
    private AtomicInteger saves;

    private InputStream originalIn;
    private PrintStream originalOut;
    private Locale originalLocale;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        originalLocale = Locale.getDefault();

        Locale.setDefault(Locale.US);   // so 12.5 prints as "12.50", not "12,50"
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));

        transactionService = new TransactionService();
        budgetService = new BudgetService();
        saves = new AtomicInteger();
        transactionController = new TransactionController(transactionService, saves::incrementAndGet);
        budgetController = new BudgetController(budgetService, transactionService, saves::incrementAndGet);
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        Locale.setDefault(originalLocale);
    }

    // ---------- helpers ----------

    // Types the given lines into the View and returns everything it printed.
    private String run(String... inputLines) {
        return run(transactionController, inputLines);
    }

    private String run(TransactionController controller, String... inputLines) {
        String input = (inputLines.length == 0) ? "" : String.join("\n", inputLines) + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        new View(controller, budgetController).run();

        System.out.flush();
        return captured.toString(StandardCharsets.UTF_8);
    }

    private void addTransaction(String id, LocalDate date, double amount,
                                String description, TransactionType type) {
        transactionService.addTransaction(new Transaction(id, date, amount, description, type));
    }

    // ====================== menu and navigation ======================

    @Test
    void menu_isShown_andExitSaysGoodbye() {
        String output = run("0");

        assertTrue(output.contains("Personal Finance Tracker"));
        assertTrue(output.contains("1. Add transaction"));
        assertTrue(output.contains("0. Exit"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void invalidMenuChoice_showsMessage_andMenuContinues() {
        String output = run("abc", "42", "0");

        assertTrue(output.contains("Invalid choice"));
        assertTrue(output.contains("Goodbye!"));   // still reached Exit afterwards
    }



    @Test
    void endOfInput_endsGracefully_withoutAnException() {
        assertDoesNotThrow(() -> run());   // no input at all
    }

    @Test
    void unexpectedError_isShown_andMenuContinues() {
        // a controller whose "save" always fails, like a full disk
        TransactionController failingSave = new TransactionController(transactionService,
                () -> { throw new IllegalStateException("disk full"); });

        String output = run(failingSave, "1", "2", "Food", "12.50", "2026-10-01", "0");

        assertTrue(output.contains("Error: disk full"));
        assertTrue(output.contains("Goodbye!"));   // the program did not crash
    }

    // ====================== add transaction ======================

    @Test
    void add_expense_isStoredAndSaved() {
        String output = run("1", "2", "Food", "12.50", "2026-10-01", "0");

        assertTrue(output.contains("Transaction added"));
        assertEquals(1, transactionService.getTransactions().size());

        Transaction t = transactionService.getTransactions().get(0);
        assertEquals("Food", t.getDescription());
        assertEquals(12.50, t.getAmount(), 0.001);
        assertEquals(TransactionType.EXPENSE, t.getTransactionType());
        assertEquals(OCT_1, t.getDate());
        assertEquals(1, saves.get());
    }

    @Test
    void add_income_isStoredAsIncome() {
        run("1", "1", "Salary", "800", "2026-10-03", "0");

        Transaction t = transactionService.getTransactions().get(0);
        assertEquals(TransactionType.INCOME, t.getTransactionType());
        assertEquals(800.00, t.getAmount(), 0.001);
    }

    @Test
    void add_blankDate_usesToday() {
        LocalDate before = LocalDate.now();
        run("1", "1", "Salary", "800", "", "0");
        LocalDate after = LocalDate.now();   // in case midnight passed mid-test

        LocalDate saved = transactionService.getTransactions().get(0).getDate();
        assertTrue(saved.equals(before) || saved.equals(after));
    }

    @Test
    void add_invalidAmount_asksAgain() {
        String output = run("1", "2", "Food", "abc", "-5", "12.50", "2026-10-01", "0");

        assertTrue(output.contains("Please enter a valid number"));
        assertTrue(output.contains("Amount must be greater than 0"));
        assertEquals(1, transactionService.getTransactions().size());
        assertEquals(12.50, transactionService.getTransactions().get(0).getAmount(), 0.001);
    }

    @Test
    void add_invalidDate_asksAgain() {
        String output = run("1", "2", "Food", "12.50", "not-a-date", "2026-10-01", "0");

        assertTrue(output.contains("Invalid date"));
        assertEquals(OCT_1, transactionService.getTransactions().get(0).getDate());
    }

    @Test
    void add_invalidType_asksAgain() {
        String output = run("1", "3", "2", "Food", "12.50", "2026-10-01", "0");

        assertTrue(output.contains("Please enter 1 or 2"));
        assertEquals(TransactionType.EXPENSE, transactionService.getTransactions().get(0).getTransactionType());
    }

    @Test
    void add_emptyDescription_asksAgain() {
        String output = run("1", "2", "", "Food", "12.50", "2026-10-01", "0");

        assertTrue(output.contains("This field cannot be empty"));
        assertEquals("Food", transactionService.getTransactions().get(0).getDescription());
    }

    // ====================== viewing ======================

    @Test
    void viewAll_showsEveryTransaction() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);
        addTransaction("T2", OCT_3, 800.00, "Salary", TransactionType.INCOME);

        String output = run("2", "0");

        assertTrue(output.contains("T1"));
        assertTrue(output.contains("Food"));
        assertTrue(output.contains("12.50"));
        assertTrue(output.contains("EXPENSE"));
        assertTrue(output.contains("T2"));
        assertTrue(output.contains("Salary"));
        assertTrue(output.contains("800.00"));
        assertTrue(output.contains("INCOME"));
    }

    @Test
    void viewAll_whenEmpty_saysSo() {
        String output = run("2", "0");

        assertTrue(output.contains("No transactions found"));
    }

    @Test
    void viewByMonth_showsOnlyThatMonth() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);
        addTransaction("T2", NOV_2, 9.00, "Transport", TransactionType.EXPENSE);

        String output = run("3", "2026-10", "0");

        assertTrue(output.contains("Transactions for 2026-10"));
        assertTrue(output.contains("Food"));
        assertFalse(output.contains("Transport"));
    }

    @Test
    void viewByMonth_invalidMonth_asksAgain() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        String output = run("3", "oct", "2026-10", "0");

        assertTrue(output.contains("Invalid month"));
        assertTrue(output.contains("Food"));
    }

    // ====================== edit ======================

    @Test
    void edit_changesChosenFields_andEnterKeepsTheRest() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        // ID, then: type (Enter = keep), description, amount, date (Enter = keep)
        String output = run("4", "T1", "", "Rent", "99", "", "0");

        assertTrue(output.contains("Transaction updated"));
        Transaction t = transactionService.getTransactions().get(0);
        assertEquals("T1", t.getTransactionId());
        assertEquals("Rent", t.getDescription());
        assertEquals(99.00, t.getAmount(), 0.001);
        assertEquals(TransactionType.EXPENSE, t.getTransactionType());   // kept
        assertEquals(OCT_1, t.getDate());                                // kept
        assertEquals(1, saves.get());
    }

    @Test
    void edit_idIsNotCaseSensitive() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        run("4", "t1", "", "Rent", "99", "", "0");

        assertEquals("Rent", transactionService.getTransactions().get(0).getDescription());
    }

    @Test
    void edit_unknownId_changesNothing() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        String output = run("4", "NOPE", "0");

        assertTrue(output.contains("No transaction with ID NOPE"));
        assertEquals("Food", transactionService.getTransactions().get(0).getDescription());
        assertEquals(0, saves.get());
    }

    // ====================== delete ======================

    @Test
    void delete_confirmed_removesTheTransaction() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        String output = run("5", "T1", "y", "0");

        assertTrue(output.contains("Transaction deleted"));
        assertTrue(transactionService.getTransactions().isEmpty());
        assertEquals(1, saves.get());
    }

    @Test
    void delete_declined_keepsTheTransaction() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        String output = run("5", "T1", "n", "0");

        assertTrue(output.contains("Cancelled"));
        assertEquals(1, transactionService.getTransactions().size());
        assertEquals(0, saves.get());
    }

    @Test
    void delete_unknownId_changesNothing() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);

        String output = run("5", "NOPE", "0");

        assertTrue(output.contains("No transaction with ID NOPE"));
        assertEquals(1, transactionService.getTransactions().size());
    }

    // ====================== totals ======================

    @Test
    void monthlySummary_showsIncomeExpenseAndBalance() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);
        addTransaction("T2", OCT_3, 800.00, "Salary", TransactionType.INCOME);
        addTransaction("T3", NOV_2, 9.00, "Transport", TransactionType.EXPENSE);   // other month

        String output = run("6", "2026-10", "0");

        assertTrue(output.contains("Summary for 2026-10"));
        assertTrue(output.contains("RM800.00"));   // income
        assertTrue(output.contains("RM12.50"));    // expense
        assertTrue(output.contains("RM787.50"));   // balance
        assertFalse(output.contains("RM9.00"));    // November is not included
    }

    @Test
    void categoryTotal_showsTheSum() {
        addTransaction("T1", OCT_1, 12.50, "Food", TransactionType.EXPENSE);
        addTransaction("T2", OCT_3, 7.50, "Food", TransactionType.EXPENSE);
        addTransaction("T3", NOV_2, 9.00, "Transport", TransactionType.EXPENSE);

        String output = run("7", "Food", "2", "0");

        assertTrue(output.contains("RM20.00"));
    }

    // ====================== budgets ======================

    @Test
    void setBudget_isStoredAndSaved() {
        String output = run("8", "2026-10", "Food", "500", "0");

        assertTrue(output.contains("Budget saved"));
        assertEquals(1, budgetService.getBudgets().size());

        Budget b = budgetService.getBudgets().get(0);
        assertEquals(YearMonth.of(2026, 10), b.getMonth());
        assertEquals("Food", b.getCategory());
        assertEquals(500.00, b.getLimit(), 0.001);
        assertEquals(1, saves.get());
    }

    @Test
    void setBudget_invalidLimit_asksAgain() {
        // "abc" and "0" are rejected, "500" is accepted, the last "0" is Exit
        String output = run("8", "2026-10", "Food", "abc", "0", "500", "0");

        assertTrue(output.contains("Please enter a valid number"));
        assertTrue(output.contains("Amount must be greater than 0"));
        assertEquals(500.00, budgetService.getBudgets().get(0).getLimit(), 0.001);
    }

    // NOTE: if you changed the wording of option 9 when you removed the
    // spent/over-budget feature, adjust the expected text here.
    @Test
    void menu_doesNotOfferTheRemovedBudgetViewOption() {
        String output = run("0");

        assertFalse(output.contains("View budget vs spent"));
    }

    @Test
    void removedOption9_isTreatedAsInvalid() {
        String output = run("9", "0");

        assertTrue(output.contains("Invalid choice"));
    }
}
