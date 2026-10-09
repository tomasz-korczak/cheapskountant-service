package pl.tomaszko.cheapskountant.expense.api;

import java.math.BigDecimal;

public record CategoryTotal(String category, String currency, BigDecimal amount) {
}
