package pl.tomaszko.cheapskountant.expense.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExpenseResponse(
        BigDecimal amount,
        LocalDate paymentDate,
        String category,
        String currency,
        String description) {
}
