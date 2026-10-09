import com.shaafee.controller.BudgetController;
import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;
import com.shaafee.model.TransactionType;
import com.shaafee.service.BudgetService;
import com.shaafee.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BudgetControllerTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private TransactionService transactionService;
    private BudgetService budgetService;
    private BudgetController controller;
    private AtomicInteger saves;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        budgetService = new BudgetService();
        saves = new AtomicInteger();
        controller = new BudgetController(budgetService, transactionService, saves::incrementAndGet);
    }

    private void addTransaction(String id, LocalDate date, double amount, String description, TransactionType type) {
        transactionService.addTransaction(new Transaction(id, date, amount, description, type));
    }

    // ---------- setBudget ----------

    @Test
    void setBudget_validInput_storesItAndSaves() {
        assertTrue(controller.setBudget(OCT, "Food", 500.00));

        assertEquals(1, budgetService.getBudgets().size());
        assertEquals(1, saves.get());
    }

    @Test
    void setBudget_trimsTheCategory() {
        controller.setBudget(OCT, "  Food  ", 500.00);

        assertEquals("Food", budgetService.getBudgets().get(0).getCategory());
    }

    @Test
    void setBudget_nullMonth_isRejected() {
        assertFalse(controller.setBudget(null, "Food", 500.00));
        assertNothingChanged();
    }

    @Test
    void setBudget_blankCategory_isRejected() {
        assertFalse(controller.setBudget(OCT, "   ", 500.00));
        assertNothingChanged();
    }

    @Test
    void setBudget_nullCategory_isRejected() {
        assertFalse(controller.setBudget(OCT, null, 500.00));
        assertNothingChanged();
    }

    @Test
    void setBudget_zeroLimit_isRejected() {
        assertFalse(controller.setBudget(OCT, "Food", 0));
        assertNothingChanged();
    }

    @Test
    void setBudget_negativeLimit_isRejected() {
        assertFalse(controller.setBudget(OCT, "Food", -100));
        assertNothingChanged();
    }

    // ---------- getBudgets ----------

    @Test
    void getBudgets_returnsOnlyTheRequestedMonth() {
        controller.setBudget(OCT, "Food", 500.00);
        controller.setBudget(OCT, "Transport", 150.00);
        controller.setBudget(NOV, "Food", 450.00);

        List<Budget> october = controller.getBudgets(OCT);

        assertEquals(2, october.size());
        assertTrue(october.stream().allMatch(b -> b.getMonth().equals(OCT)));
    }

    @Test
    void getBudgets_monthWithNoBudgets_returnsEmptyList() {
        controller.setBudget(OCT, "Food", 500.00);

        assertTrue(controller.getBudgets(NOV).isEmpty());
    }

    @Test
    void getBudgets_nullMonth_returnsEmptyList() {
        controller.setBudget(OCT, "Food", 500.00);

        assertTrue(controller.getBudgets(null).isEmpty());
    }

    private void assertNothingChanged() {
        assertTrue(budgetService.getBudgets().isEmpty());
        assertEquals(0, saves.get());
    }
}
