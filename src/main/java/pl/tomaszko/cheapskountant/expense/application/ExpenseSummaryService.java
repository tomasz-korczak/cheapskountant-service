package pl.tomaszko.cheapskountant.expense.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

import org.springframework.stereotype.Service;

import pl.tomaszko.cheapskountant.expense.api.CategoryTotal;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;

@Service
public class ExpenseSummaryService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);

    private final ExpenseRepository expenses;

    public ExpenseSummaryService(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    public List<CategoryTotal> summarize(String from, String to) {
        LocalDate start = date(from);
        LocalDate end = date(to);
        if (start.isAfter(end)) {
            throw ReceiptFailureException.invalidSubmission("The first date is after the last date.");
        }
        try {
            return expenses.summarize(start, end).stream()
                    .map(total -> new CategoryTotal(total.category(), total.currency(), scale(total.amount())))
                    .toList();
        }
        catch (ReceiptFailureException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw ReceiptFailureException.storageFailed("The expenses could not be read.", ex);
        }
    }

    private LocalDate date(String value) {
        if (value == null || value.length() != 10) {
            throw ReceiptFailureException.invalidSubmission("A summary date is not a real calendar date.");
        }
        try {
            return LocalDate.parse(value, DATE);
        }
        catch (DateTimeParseException ex) {
            throw ReceiptFailureException.invalidSubmission("A summary date is not a real calendar date.");
        }
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
