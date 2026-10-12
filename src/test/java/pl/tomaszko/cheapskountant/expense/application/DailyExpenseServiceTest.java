package pl.tomaszko.cheapskountant.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import pl.tomaszko.cheapskountant.expense.api.DailyExpense;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.application.CreateReceiptService;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class DailyExpenseServiceTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.8");

    @Autowired
    private DailyExpenseService dailyExpenseService;

    @Autowired
    private StoreExpensesService storeExpensesService;

    @Autowired
    private CreateReceiptService createReceiptService;

    @Autowired
    private ExpenseRepository expenses;

    @Test
    void listsStandaloneExpensesAndOneTotalPerReceipt() {
        StoredReceiptResponse biedronka = createReceiptService.create(ReceiptFixtures.withoutId(ReceiptFixtures.discountedDraft()));
        StoredReceiptResponse lidl = createReceiptService.create(ReceiptFixtures.withoutId(sameDayLidl()));
        StoredReceiptResponse sklep = createReceiptService.create(ReceiptFixtures.withoutId(ReceiptFixtures.validDraft()));
        storeExpensesService.store(List.of(
                new ExpenseSubmission(new BigDecimal("12.5"), "2026-09-29", "Jedzenie", "PLN", "chleb"),
                new ExpenseSubmission(new BigDecimal("3.00"), "2026-09-29", "Browar", "PLN", null),
                new ExpenseSubmission(new BigDecimal("9.00"), "2026-09-30", "Kino", "PLN", null)));

        List<ExpenseEntity> standalone = expenses.findAll().stream()
                .filter(expense -> expense.receiptItem() == null && LocalDate.parse("2026-09-29").equals(expense.paymentDate()))
                .sorted(Comparator.comparing(ExpenseEntity::id))
                .toList();

        assertThat(dailyExpenseService.list("2026-09-29")).containsExactly(
                new DailyExpense("expense", LocalDate.parse("2026-09-29"), standalone.get(0).id(), "Jedzenie", new BigDecimal("12.50")),
                new DailyExpense("expense", LocalDate.parse("2026-09-29"), standalone.get(1).id(), "Browar", new BigDecimal("3.00")),
                new DailyExpense("receipt", LocalDate.parse("2026-09-29"), biedronka.id(), "BIEDRONKA", new BigDecimal("23.87")),
                new DailyExpense("receipt", LocalDate.parse("2026-09-29"), lidl.id(), "Lidl", new BigDecimal("4.00")));
        assertThat(dailyExpenseService.list("2026-09-30")).singleElement()
                .satisfies(row -> {
                    assertThat(row.type()).isEqualTo("expense");
                    assertThat(row.name()).isEqualTo("Kino");
                    assertThat(row.amount()).isEqualByComparingTo("9.00");
                });
        assertThat(dailyExpenseService.list("2024-05-01")).containsExactly(
                new DailyExpense("receipt", LocalDate.parse("2024-05-01"), sklep.id(), "Sklep", new BigDecimal("4.00")));
        assertThat(dailyExpenseService.list("2026-01-01")).isEmpty();
    }

    @Test
    void rejectsAMissingOrImpossiblePaymentDate() {
        assertThatThrownBy(() -> dailyExpenseService.list(null))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
        assertThatThrownBy(() -> dailyExpenseService.list("2026-02-31"))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
    }

    private ReceiptDraft sameDayLidl() {
        ReceiptDraft base = ReceiptFixtures.validDraft();
        return new ReceiptDraft(
                base.documentType(),
                base.source(),
                new ReceiptDraft.Seller("Lidl", null, "1234567890", null, null, null),
                new ReceiptDraft.ReceiptInfo("R2", OffsetDateTime.parse("2026-09-29T08:00:00Z"), "PLN", null),
                base.items(),
                base.discountSummary(),
                base.taxSummary(),
                base.totals(),
                base.payments(),
                base.fiscalData(),
                base.unparsedLines());
    }
}
