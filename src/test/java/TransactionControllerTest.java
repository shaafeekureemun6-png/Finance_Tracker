import com.shaafee.controller.TransactionController;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TransactionControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    private TransactionService service;
    private TransactionController controller;
    private AtomicInteger saves;   // counts how many times the controller "saved"

    @BeforeEach
    void setUp() {
        service = new TransactionService();
        saves = new AtomicInteger();
        controller = new TransactionController(service, saves::incrementAndGet);
    }

    private void addSample(String id) {
        service.addTransaction(new Transaction(id, DATE, 12.50, "Food", TransactionType.EXPENSE));
    }

    // ---------- add ----------

    @Test
    void addTransaction_validInput_addsItAndSaves() {
        boolean result = controller.addTransaction("T1", DATE, 12.50, "Food", TransactionType.EXPENSE);

        assertTrue(result);
        assertEquals(1, service.getTransactions().size());
        assertEquals(1, saves.get());
    }

    @Test
    void addTransaction_trimsTheDescription() {
        controller.addTransaction("T1", DATE, 12.50, "  Food  ", TransactionType.EXPENSE);

        assertEquals("Food", service.getTransactions().get(0).getDescription());
    }

    @Test
    void addTransaction_zeroAmount_isRejected() {
        assertFalse(controller.addTransaction("T1", DATE, 0, "Food", TransactionType.EXPENSE));
        assertNothingChanged();
    }

    @Test
    void addTransaction_negativeAmount_isRejected() {
        assertFalse(controller.addTransaction("T1", DATE, -5, "Food", TransactionType.EXPENSE));
        assertNothingChanged();
    }

    @Test
    void addTransaction_blankDescription_isRejected() {
        assertFalse(controller.addTransaction("T1", DATE, 10, "   ", TransactionType.EXPENSE));
        assertNothingChanged();
    }

    @Test
    void addTransaction_nullDescription_isRejected() {
        assertFalse(controller.addTransaction("T1", DATE, 10, null, TransactionType.EXPENSE));
        assertNothingChanged();
    }

    @Test
    void addTransaction_nullDate_isRejected() {
        assertFalse(controller.addTransaction("T1", null, 10, "Food", TransactionType.EXPENSE));
        assertNothingChanged();
    }

    @Test
    void addTransaction_nullType_isRejected() {
        assertFalse(controller.addTransaction("T1", DATE, 10, "Food", null));
        assertNothingChanged();
    }

    @Test
    void addTransaction_blankId_isRejected() {
        assertFalse(controller.addTransaction(" ", DATE, 10, "Food", TransactionType.EXPENSE));
        assertNothingChanged();
    }

    // ---------- update ----------

    @Test
    void updateTransaction_existingId_updatesAndSaves() {
        addSample("T1");

        boolean result = controller.updateTransaction("T1", DATE, 99.00, "Rent", TransactionType.EXPENSE);

        assertTrue(result);
        Transaction updated = controller.findById("T1").get();
        assertEquals(99.00, updated.getAmount(), 0.001);
        assertEquals("Rent", updated.getDescription());
        assertEquals(1, saves.get());
    }

    @Test
    void updateTransaction_unknownId_returnsFalseAndDoesNotSave() {
        addSample("T1");

        assertFalse(controller.updateTransaction("NOPE", DATE, 99.00, "Rent", TransactionType.EXPENSE));
        assertEquals(0, saves.get());
    }

    @Test
    void updateTransaction_invalidValues_isRejectedAndKeepsTheOldData() {
        addSample("T1");

        assertFalse(controller.updateTransaction("T1", DATE, -1, "Rent", TransactionType.EXPENSE));

        assertEquals(12.50, controller.findById("T1").get().getAmount(), 0.001);
        assertEquals(0, saves.get());
    }

    // ---------- delete ----------

    @Test
    void deleteTransaction_existingId_removesAndSaves() {
        addSample("T1");

        assertTrue(controller.deleteTransaction("T1"));

        assertTrue(service.getTransactions().isEmpty());
        assertEquals(1, saves.get());
    }

    @Test
    void deleteTransaction_unknownId_returnsFalseAndDoesNotSave() {
        addSample("T1");

        assertFalse(controller.deleteTransaction("NOPE"));

        assertEquals(1, service.getTransactions().size());
        assertEquals(0, saves.get());
    }

    // ---------- reading ----------

    @Test
    void getAllTransactions_returnsEveryTransaction() {
        addSample("T1");
        addSample("T2");
        addSample("T3");

        assertEquals(3, controller.getAllTransactions().size());
    }

    @Test
    void findById_existingId_isPresent() {
        addSample("T1");

        assertTrue(controller.findById("T1").isPresent());
    }

    @Test
    void findById_unknownId_isEmpty() {
        addSample("T1");

        assertTrue(controller.findById("NOPE").isEmpty());
    }

    @Test
    void getMonthlyTransaction_returnsOnlyThatMonth() {
        service.addTransaction(new Transaction("T1", LocalDate.of(2026, 10, 1), 1, "Food", TransactionType.EXPENSE));
        service.addTransaction(new Transaction("T2", LocalDate.of(2026, 11, 1), 2, "Food", TransactionType.EXPENSE));

        assertEquals(1, controller.getMonthlyTransaction(YearMonth.of(2026, 10)).size());
    }

    @Test
    void getMonthlyTransaction_nullMonth_returnsEmptyList() {
        addSample("T1");

        assertTrue(controller.getMonthlyTransaction(null).isEmpty());
    }

    // ---------- totals ----------

    @Test
    void getMonthlyTotal_sumsTheRightTypeAndMonth() {
        service.addTransaction(new Transaction("T1", LocalDate.of(2026, 10, 1), 12.50, "Food", TransactionType.EXPENSE));
        service.addTransaction(new Transaction("T2", LocalDate.of(2026, 10, 3), 800.00, "Salary", TransactionType.INCOME));
        service.addTransaction(new Transaction("T3", LocalDate.of(2026, 11, 2), 9.00, "Food", TransactionType.EXPENSE));

        YearMonth oct = YearMonth.of(2026, 10);
        assertEquals(12.50, controller.getMonthlyTotal(oct, TransactionType.EXPENSE), 0.001);
        assertEquals(800.00, controller.getMonthlyTotal(oct, TransactionType.INCOME), 0.001);
    }

    @Test
    void getMonthlyTotal_nullArguments_returnZero() {
        addSample("T1");

        assertEquals(0.0, controller.getMonthlyTotal(null, TransactionType.EXPENSE), 0.001);
        assertEquals(0.0, controller.getMonthlyTotal(YearMonth.of(2026, 10), null), 0.001);
    }

    @Test
    void getCategoryTotal_sumsMatchingDescriptions() {
        addSample("T1");
        addSample("T2");

        assertEquals(25.00, controller.getCategoryTotal("Food", TransactionType.EXPENSE), 0.001);
    }

    @Test
    void getCategoryTotal_nullArguments_returnZero() {
        addSample("T1");

        assertEquals(0.0, controller.getCategoryTotal(null, TransactionType.EXPENSE), 0.001);
        assertEquals(0.0, controller.getCategoryTotal("Food", null), 0.001);
    }

    // ---------- helper ----------

    private void assertNothingChanged() {
        assertTrue(service.getTransactions().isEmpty());
        assertEquals(0, saves.get());
    }
}
