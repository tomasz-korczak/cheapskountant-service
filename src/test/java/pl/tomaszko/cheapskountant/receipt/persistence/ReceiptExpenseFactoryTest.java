package pl.tomaszko.cheapskountant.receipt.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;

@ExtendWith(MockitoExtension.class)
class ReceiptExpenseFactoryTest {

    @Mock
    private ExpenseRepository expenses;

    private final ReceiptExpenseFactory factory = new ReceiptExpenseFactory();

    @Test
    void skipsALineThatAlreadyHasAnExpense() {
        ReceiptEntity receipt = receipt(item(7L, "4.00", null));
        when(expenses.existsByReceiptItem_Id(7L)).thenReturn(true);

        assertThat(factory.missing(receipt, expenses)).isEmpty();
    }

    @Test
    void treatsAMissingDiscountAsZero() {
        ReceiptEntity receipt = receipt(item(null, "4.00", null));

        ExpenseEntity expense = factory.missing(receipt, expenses).get(0);

        assertThat(expense.amount()).isEqualByComparingTo("4.00");
        assertThat(expense.paymentDate()).isEqualTo(LocalDate.parse("2026-09-29"));
        assertThat(expense.description()).isNull();
        assertThat(expense.receiptItem()).isSameAs(receipt.items.get(0));
    }

    @Test
    void rejectsALineWhoseNetAmountDoesNotFit() {
        ReceiptEntity receipt = receipt(item(null, "1000000.00", null));

        assertThatThrownBy(() -> factory.rejectOversized(receipt))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
    }

    private static ReceiptEntity receipt(ReceiptItemEntity item) {
        ReceiptEntity receipt = new ReceiptEntity();
        ReceiptHeaderEntity header = new ReceiptHeaderEntity();
        header.issuedAt = LocalDateTime.parse("2026-09-29T20:02:00");
        header.currency = "PLN";
        receipt.header = header;
        receipt.items.add(item);
        return receipt;
    }

    private static ReceiptItemEntity item(Long id, String lineTotal, BigDecimal discountTotal) {
        ReceiptItemEntity item = new ReceiptItemEntity();
        item.id = id;
        item.lineTotal = new BigDecimal(lineTotal);
        item.discountTotal = discountTotal;
        item.category = new ExpenseCategoryEntity("Jedzenie");
        return item;
    }
}
