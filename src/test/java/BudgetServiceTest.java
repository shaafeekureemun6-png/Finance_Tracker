import com.shaafee.model.Budget;
import com.shaafee.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {

    private BudgetService service;

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    @BeforeEach
    void setUp() {
        service = new BudgetService();
    }

    @Test
    void setBudget_storesIt() {
        assertTrue(service.setBudget(new Budget(OCT, "Food", 500.00)));

        assertEquals(1, service.getBudgets().size());
        assertEquals("Food", service.getBudgets().get(0).getCategory());
        assertEquals(500.00, service.getBudgets().get(0).getLimit(), 0.001);
    }

    @Test
    void setBudget_differentCategoriesSameMonth_areBothKept() {
        service.setBudget(new Budget(OCT, "Food", 500.00));
        service.setBudget(new Budget(OCT, "Transport", 150.00));

        assertEquals(2, service.getBudgets().size());
    }

    @Test
    void setBudget_sameCategoryDifferentMonths_areBothKept() {
        service.setBudget(new Budget(OCT, "Food", 500.00));
        service.setBudget(new Budget(NOV, "Food", 450.00));

        assertEquals(2, service.getBudgets().size());
    }

    // KNOWN BUG: fails until setBudget replaces an existing budget with the
    // same month and category instead of always adding a new one.
    @Test
    void setBudget_sameMonthAndCategory_replacesTheOldOne() {
        service.setBudget(new Budget(OCT, "Food", 500.00));
        service.setBudget(new Budget(OCT, "Food", 600.00));

        assertEquals(1, service.getBudgets().size());
        assertEquals(600.00, service.getBudgets().get(0).getLimit(), 0.001);
    }
}
