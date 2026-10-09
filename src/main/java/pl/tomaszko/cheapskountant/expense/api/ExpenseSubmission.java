package pl.tomaszko.cheapskountant.expense.api;

import java.math.BigDecimal;

public record ExpenseSubmission(
        BigDecimal amount,
        String paymentDate,
        String category,
        String currency,
        String description) {
}
