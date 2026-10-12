package pl.tomaszko.cheapskountant.expense.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyExpense(String type, LocalDate date, Long id, String name, BigDecimal amount) {
}
