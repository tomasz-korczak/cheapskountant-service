package pl.tomaszko.cheapskountant.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;

@ExtendWith(MockitoExtension.class)
class ExpenseSubmissionValidatorTest {

    @Mock
    private ExpenseCategoryRepository categories;

    private ExpenseSubmissionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ExpenseSubmissionValidator(categories);
        lenient().when(categories.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            if (HouseholdCategories.household(name) || HouseholdCategories.UNKNOWN.equals(name)) {
                return Optional.of(new ExpenseCategoryEntity(name));
            }
            return Optional.empty();
        });
    }

    @Test
    void acceptsACompleteExpenseAndKeepsScale() {
        List<ExpenseEntity> stored = validator.validate(List.of(expense(new BigDecimal("12.5"), "2026-10-10", "Jedzenie", "eur", null)));

        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).amount()).isEqualByComparingTo("12.50");
        assertThat(stored.get(0).amount().scale()).isEqualTo(2);
        assertThat(stored.get(0).currency()).isEqualTo("eur");
        assertThat(stored.get(0).description()).isNull();
        assertThat(validator.validate(List.of(expense(new BigDecimal("0"), "2099-01-01", "Jedzenie", "EUR", ""))).get(0).amount())
                .isEqualByComparingTo("0.00");
        assertThat(validator.validate(List.of(expense(new BigDecimal("-1.2"), "2026-10-10", "Browar", "PLN", "a"))).get(0).amount())
                .isEqualByComparingTo("-1.20");
    }

    @Test
    void acceptsFiveHundredAndAHundredCharacterDescription() {
        List<ExpenseEntity> stored = validator.validate(copies(500, expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PLN", "a".repeat(100))));

        assertThat(stored).hasSize(500);
    }

    @Test
    void rejectsAnEmptyListAndAListOf501() {
        assertInvalid(List.of());
        assertInvalid(null);
        assertInvalid(copies(501, expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PLN", null)));
    }

    @Test
    void rejectsAMissingFieldOrAnInvalidValue() {
        assertInvalid(List.of(new ExpenseSubmission(null, "2026-10-10", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(new ExpenseSubmission(new BigDecimal("1"), null, "Jedzenie", "PLN", null)));
        assertInvalid(List.of(new ExpenseSubmission(new BigDecimal("1"), "2026-10-10", null, "PLN", null)));
        assertInvalid(List.of(new ExpenseSubmission(new BigDecimal("1"), "2026-10-10", "Jedzenie", null, null)));
        assertInvalid(List.of(expense(new BigDecimal("1000000.00"), "2026-10-10", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("-999999.999"), "2026-10-10", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1.001"), "2026-10-10", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-02-31", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10T00:00:00", "Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PL", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "EUR ", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", " Jedzenie", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "Pieniądze, po prostu...", "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", HouseholdCategories.UNKNOWN, "PLN", null)));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PLN", " ")));
        assertInvalid(List.of(expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PLN", "a".repeat(101))));
    }

    @Test
    void oneInvalidExpenseRejectsTheWholeList() {
        assertInvalid(List.of(
                expense(new BigDecimal("1"), "2026-10-10", "Jedzenie", "PLN", "ok"),
                expense(new BigDecimal("1"), "2026-10-10", HouseholdCategories.UNKNOWN, "PLN", null)));
    }

    private void assertInvalid(List<ExpenseSubmission> expenses) {
        assertThatThrownBy(() -> validator.validate(expenses))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
    }

    private List<ExpenseSubmission> copies(int count, ExpenseSubmission expense) {
        List<ExpenseSubmission> expenses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            expenses.add(expense);
        }
        return expenses;
    }

    private ExpenseSubmission expense(BigDecimal amount, String paymentDate, String category, String currency, String description) {
        return new ExpenseSubmission(amount, paymentDate, category, currency, description);
    }
}
