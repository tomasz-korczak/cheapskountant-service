package pl.tomaszko.cheapskountant.expense.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.api.ExpenseResponse;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.expense.application.StoreExpensesService;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class ExpensePersistenceTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.8");

    @Autowired
    private StoreExpensesService storeExpensesService;

    @Autowired
    private ExpenseRepository expenses;

    @Autowired
    private ExpenseCategoryRepository categories;

    @Test
    void storesBatchesThatShareAPaymentDate() {
        List<ExpenseResponse> first = storeExpensesService.store(List.of(
                new ExpenseSubmission(new BigDecimal("12.5"), "2026-10-10", "Jedzenie", "PLN", "chleb"),
                new ExpenseSubmission(new BigDecimal("3.5"), "2026-10-10", "Jedzenie", "EUR", null)));
        storeExpensesService.store(List.of(new ExpenseSubmission(new BigDecimal("1.00"), "2026-10-10", "Browar", "PLN", "")));
        storeExpensesService.store(List.of(new ExpenseSubmission(new BigDecimal("2.00"), "2026-10-10", "Kino", "PLN", null)));
        storeExpensesService.store(List.of(new ExpenseSubmission(new BigDecimal("4.00"), "2026-10-10", "Telefon", "PLN", null)));

        assertThat(first).extracting(ExpenseResponse::category).containsExactly("Jedzenie", "Jedzenie");
        assertThat(first.get(0).amount()).isEqualByComparingTo("12.50");
        assertThat(first.get(0).amount().scale()).isEqualTo(2);
        assertThat(first.get(0).description()).isEqualTo("chleb");
        assertThat(first.get(1).description()).isNull();
        assertThat(first.get(1).currency()).isEqualTo("EUR");
        assertThat(expenses.countByPaymentDate(LocalDate.parse("2026-10-10"))).isEqualTo(5);
        assertThat(categories.findByName("Jedzenie")).get().extracting(ExpenseCategoryEntity::name).isEqualTo("Jedzenie");
    }

    @Test
    void oneInvalidExpenseStoresNothingFromThatRequest() {
        storeExpensesService.store(List.of(new ExpenseSubmission(new BigDecimal("1.00"), "2026-10-11", "Jedzenie", "PLN", null)));

        assertThatThrownBy(() -> storeExpensesService.store(List.of(
                new ExpenseSubmission(new BigDecimal("2.00"), "2026-10-11", "Jedzenie", "PLN", null),
                new ExpenseSubmission(new BigDecimal("3.00"), "2026-10-11", HouseholdCategories.UNKNOWN, "PLN", null))))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);

        assertThat(expenses.countByPaymentDate(LocalDate.parse("2026-10-11"))).isEqualTo(1);
    }
}
