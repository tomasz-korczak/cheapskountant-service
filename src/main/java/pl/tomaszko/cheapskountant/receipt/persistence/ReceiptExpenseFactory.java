package pl.tomaszko.cheapskountant.receipt.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;

@Component
public class ReceiptExpenseFactory {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-999999.99");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

    public void rejectOversized(ReceiptEntity receipt) {
        for (ReceiptItemEntity item : receipt.items) {
            amount(item);
        }
    }

    public List<ExpenseEntity> missing(ReceiptEntity receipt, ExpenseRepository expenses) {
        List<ExpenseEntity> created = new ArrayList<>();
        for (ReceiptItemEntity item : receipt.items) {
            if (item.id != null && expenses.existsByReceiptItem_Id(item.id)) {
                continue;
            }
            created.add(new ExpenseEntity(
                    amount(item),
                    receipt.header.issuedAt.toLocalDate(),
                    item.category,
                    receipt.header.currency,
                    null,
                    item));
        }
        return created;
    }

    private BigDecimal amount(ReceiptItemEntity item) {
        BigDecimal discount = item.discountTotal == null ? BigDecimal.ZERO : item.discountTotal;
        BigDecimal amount = item.lineTotal.add(discount).setScale(2, RoundingMode.UNNECESSARY);
        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw ReceiptFailureException.invalidSubmission("A receipt line amount does not fit an expense.");
        }
        return amount;
    }
}
