package pl.tomaszko.cheapskountant.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import pl.tomaszko.cheapskountant.expense.api.CategoryTotal;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class ExpenseSummaryServiceTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.8");

    @Autowired
    private StoreExpensesService storeExpensesService;

    @Autowired
    private ExpenseSummaryService expenseSummaryService;

    @Test
    void groupsByCategoryAndCurrencyInsideTheRange() {
        storeExpensesService.store(List.of(
                new ExpenseSubmission(new BigDecimal("1.00"), "2026-10-09", "Jedzenie", "PLN", "before"),
                new ExpenseSubmission(new BigDecimal("10.00"), "2026-10-10", "Jedzenie", "PLN", "bread"),
                new ExpenseSubmission(new BigDecimal("-10.00"), "2026-10-10", "Jedzenie", "PLN", null),
                new ExpenseSubmission(new BigDecimal("3.50"), "2026-10-11", "Jedzenie", "EUR", null),
                new ExpenseSubmission(new BigDecimal("1.25"), "2026-10-11", "Jedzenie", "eur", null),
                new ExpenseSubmission(new BigDecimal("5.00"), "2026-10-13", "Browar", "PLN", "after")));

        List<CategoryTotal> totals = expenseSummaryService.summarize("2026-10-10", "2026-10-12");

        assertThat(totals).containsExactlyInAnyOrder(
                new CategoryTotal("Jedzenie", "PLN", new BigDecimal("0.00")),
                new CategoryTotal("Jedzenie", "EUR", new BigDecimal("3.50")),
                new CategoryTotal("Jedzenie", "eur", new BigDecimal("1.25")));
        assertThat(totals).noneMatch(total -> "Browar".equals(total.category()));
        assertThat(totals).allMatch(total -> total.amount().scale() == 2);
        assertThat(expenseSummaryService.summarize("2026-10-10", "2026-10-10"))
                .containsExactly(new CategoryTotal("Jedzenie", "PLN", new BigDecimal("0.00")));
        assertThat(expenseSummaryService.summarize("2026-01-01", "2026-01-02")).isEmpty();
    }

    @Test
    void rejectsAMissingDateAnImpossibleDateAndAReversedRange() {
        assertInvalid(null, "2026-10-10");
        assertInvalid("2026-10-10", null);
        assertInvalid("2026-02-31", "2026-03-01");
        assertInvalid("2026-10-12", "2026-10-10");
    }

    private void assertInvalid(String from, String to) {
        assertThatThrownBy(() -> expenseSummaryService.summarize(from, to))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
    }
}
