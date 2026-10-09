package pl.tomaszko.cheapskountant.expense.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;

@Component
public class ExpenseSubmissionValidator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-999999.99");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

    private final ExpenseCategoryRepository categories;

    public ExpenseSubmissionValidator(ExpenseCategoryRepository categories) {
        this.categories = categories;
    }

    public List<ExpenseEntity> validate(List<ExpenseSubmission> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            throw ReceiptFailureException.invalidSubmission("An expense list is required.");
        }
        if (expenses.size() > 500) {
            throw ReceiptFailureException.invalidSubmission("At most 500 expenses can be stored at once.");
        }
        List<ExpenseEntity> prepared = new ArrayList<>();
        for (ExpenseSubmission expense : expenses) {
            prepared.add(one(expense));
        }
        return prepared;
    }

    private ExpenseEntity one(ExpenseSubmission expense) {
        if (expense == null || expense.amount() == null || expense.paymentDate() == null
                || expense.category() == null || expense.currency() == null) {
            throw ReceiptFailureException.invalidSubmission("An expense is missing a required field.");
        }
        BigDecimal amount = amount(expense.amount());
        LocalDate paymentDate = date(expense.paymentDate());
        ExpenseCategoryEntity category = category(expense.category());
        String currency = currency(expense.currency());
        String description = description(expense.description());
        return new ExpenseEntity(amount, paymentDate, category, currency, description);
    }

    private BigDecimal amount(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 2) {
            throw ReceiptFailureException.invalidSubmission("An expense amount has more than two decimal places.");
        }
        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw ReceiptFailureException.invalidSubmission("An expense amount is too large.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private LocalDate date(String paymentDate) {
        if (paymentDate.length() != 10) {
            throw ReceiptFailureException.invalidSubmission("An expense payment date is not a real calendar date.");
        }
        try {
            return LocalDate.parse(paymentDate, DATE);
        }
        catch (DateTimeParseException ex) {
            throw ReceiptFailureException.invalidSubmission("An expense payment date is not a real calendar date.");
        }
    }

    private ExpenseCategoryEntity category(String name) {
        if (!HouseholdCategories.household(name)) {
            throw ReceiptFailureException.invalidSubmission("An expense category is not a household name.");
        }
        return categories.findByName(name)
                .filter(found -> name.equals(found.name()))
                .orElseThrow(() -> ReceiptFailureException.invalidSubmission("An expense category is not a household name."));
    }

    private String currency(String currency) {
        if (currency.length() != 3) {
            throw ReceiptFailureException.invalidSubmission("An expense currency must be three characters.");
        }
        return currency;
    }

    private String description(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        if (description.length() > 100) {
            throw ReceiptFailureException.invalidSubmission("An expense description is too long.");
        }
        if (description.chars().allMatch(ch -> ch == ' ')) {
            throw ReceiptFailureException.invalidSubmission("An expense description must contain a character other than a space.");
        }
        return description;
    }
}
