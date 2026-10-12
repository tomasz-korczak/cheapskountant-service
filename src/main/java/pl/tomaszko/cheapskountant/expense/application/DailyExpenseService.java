package pl.tomaszko.cheapskountant.expense.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import pl.tomaszko.cheapskountant.expense.api.DailyExpense;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;

@Service
public class DailyExpenseService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);

    private final ExpenseRepository expenses;

    public DailyExpenseService(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    public List<DailyExpense> list(String paymentDate) {
        LocalDate date = date(paymentDate);
        try {
            return Stream.concat(
                            expenses.findStandaloneByPaymentDate(date).stream(),
                            expenses.summarizeReceiptsByPaymentDate(date).stream())
                    .map(this::scaled)
                    .toList();
        }
        catch (ReceiptFailureException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw ReceiptFailureException.storageFailed("The expenses could not be read.", ex);
        }
    }

    private DailyExpense scaled(DailyExpense row) {
        return new DailyExpense(row.type(), row.date(), row.id(), row.name(), scale(row.amount()));
    }

    private LocalDate date(String value) {
        if (value == null || value.length() != 10) {
            throw ReceiptFailureException.invalidSubmission("A payment date is not a real calendar date.");
        }
        try {
            return LocalDate.parse(value, DATE);
        }
        catch (DateTimeParseException ex) {
            throw ReceiptFailureException.invalidSubmission("A payment date is not a real calendar date.");
        }
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
