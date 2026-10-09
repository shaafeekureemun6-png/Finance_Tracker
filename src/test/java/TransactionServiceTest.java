import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService service;

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    // Runs before EVERY test, so each test starts with the same fresh data.
    @BeforeEach
    void setUp() {
        service = new TransactionService();
        service.addTransaction(new Transaction("T1", LocalDate.of(2026, 10, 1), 12.50, "Food", TransactionType.EXPENSE));
        service.addTransaction(new Transaction("T2", LocalDate.of(2026, 10, 3), 800.00, "Salary", TransactionType.INCOME));
        service.addTransaction(new Transaction("T3", LocalDate.of(2026, 11, 2), 9.00, "Transport", TransactionType.EXPENSE));
        service.addTransaction(new Transaction("T4", LocalDate.of(2026, 10, 5), 7.50, "Food", TransactionType.EXPENSE));
    }

    // ---------- add ----------

    @Test
    void addTransaction_storesIt() {
        assertEquals(4, service.getTransactions().size());

        service.addTransaction(new Transaction("T5", LocalDate.of(2026, 10, 9), 20.00, "Food", TransactionType.EXPENSE));

        assertEquals(5, service.getTransactions().size());
    }

    // ---------- delete ----------

    @Test
    void deleteTransaction_existingId_removesItAndReturnsTrue() {
        assertTrue(service.deleteTransaction("T1"));
        assertEquals(3, service.getTransactions().size());
    }

    @Test
    void deleteTransaction_unknownId_returnsFalseAndChangesNothing() {
        assertFalse(service.deleteTransaction("NOPE"));
        assertEquals(4, service.getTransactions().size());
    }

    // ---------- update ----------

    @Test
    void updateTransaction_unknownId_returnsFalse() {
        Transaction replacement = new Transaction("X", LocalDate.of(2026, 10, 1), 1.0, "Food", TransactionType.EXPENSE);

        assertFalse(service.updateTransaction("NOPE", replacement));
        assertEquals(4, service.getTransactions().size());
    }

    @Test
    void updateTransaction_changesTheValues() {
        Transaction updated = new Transaction("T1", LocalDate.of(2026, 10, 1), 99.00, "Food", TransactionType.EXPENSE);

        assertTrue(service.updateTransaction("T1", updated));

        double amount = service.getTransactions().stream()
                .filter(t -> t.getTransactionId().equals("T1"))
                .findFirst().get()
                .getAmount();
        assertEquals(99.00, amount, 0.001);
        assertEquals(4, service.getTransactions().size());   // replaced, not added
    }

    // KNOWN BUG: this fails until updateTransaction replaces in place
    // (list.set) instead of removing and re-adding at the end.
    @Test
    void updateTransaction_keepsTheSamePositionInTheList() {
        Transaction updated = new Transaction("T1", LocalDate.of(2026, 10, 1), 99.00, "Food", TransactionType.EXPENSE);

        service.updateTransaction("T1", updated);

        assertEquals("T1", service.getTransactions().get(0).getTransactionId());
    }

    // ---------- month filter ----------

    @Test
    void getMonthlyTransaction_returnsOnlyThatMonth() {
        assertEquals(3, service.getMonthlyTransaction(OCT).size());
        assertEquals(1, service.getMonthlyTransaction(NOV).size());
    }

    @Test
    void getMonthlyTransaction_monthWithNoData_returnsEmptyList() {
        assertTrue(service.getMonthlyTransaction(DEC).isEmpty());
    }

    // ---------- monthly totals ----------

    @Test
    void getMonthlytotal_sumsExpensesForTheMonth() {
        // October expenses: 12.50 + 7.50
        assertEquals(20.00, service.getMonthlytotal(OCT, TransactionType.EXPENSE), 0.001);
    }

    @Test
    void getMonthlytotal_sumsIncomeForTheMonth() {
        assertEquals(800.00, service.getMonthlytotal(OCT, TransactionType.INCOME), 0.001);
    }

    @Test
    void getMonthlytotal_ignoresOtherMonths() {
        assertEquals(9.00, service.getMonthlytotal(NOV, TransactionType.EXPENSE), 0.001);
    }

    @Test
    void getMonthlytotal_monthWithNoData_isZero() {
        assertEquals(0.0, service.getMonthlytotal(DEC, TransactionType.EXPENSE), 0.001);
    }

    // ---------- category totals ----------

    @Test
    void getCategoryTotal_allMonths_matchesOnDescription() {
        assertEquals(20.00, service.getCategoryTotal("Food", TransactionType.EXPENSE), 0.001);
        assertEquals(9.00, service.getCategoryTotal("Transport", TransactionType.EXPENSE), 0.001);
    }

    @Test
    void getCategoryTotal_respectsTheType() {
        // "Food" has no income entries
        assertEquals(0.0, service.getCategoryTotal("Food", TransactionType.INCOME), 0.001);
    }

    // KNOWN BUG: fails until the old two-argument version uses equalsIgnoreCase.
    @Test
    void getCategoryTotal_allMonths_isCaseInsensitive() {
        assertEquals(20.00, service.getCategoryTotal("food", TransactionType.EXPENSE), 0.001);
    }


}
