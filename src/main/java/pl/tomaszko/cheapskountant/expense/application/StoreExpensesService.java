package pl.tomaszko.cheapskountant.expense.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pl.tomaszko.cheapskountant.expense.api.ExpenseResponse;
import pl.tomaszko.cheapskountant.expense.api.ExpenseSubmission;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;

@Service
public class StoreExpensesService {

    private final ExpenseSubmissionValidator validator;
    private final ExpenseRepository expenses;
    private final TransactionTemplate transactionTemplate;

    public StoreExpensesService(
            ExpenseSubmissionValidator validator,
            ExpenseRepository expenses,
            PlatformTransactionManager transactionManager) {
        this.validator = validator;
        this.expenses = expenses;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<ExpenseResponse> store(List<ExpenseSubmission> submissions) {
        List<ExpenseEntity> prepared = validator.validate(submissions);
        try {
            List<ExpenseResponse> stored = transactionTemplate.execute(status -> {
                expenses.saveAll(prepared);
                return prepared.stream().map(this::response).toList();
            });
            if (stored == null) {
                throw ReceiptFailureException.storageFailed("The expenses could not be saved.", null);
            }
            return stored;
        }
        catch (ReceiptFailureException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw ReceiptFailureException.storageFailed("The expenses could not be saved.", ex);
        }
    }

    private ExpenseResponse response(ExpenseEntity expense) {
        return new ExpenseResponse(
                expense.amount(),
                expense.paymentDate(),
                expense.category().name(),
                expense.currency(),
                expense.description());
    }
}
