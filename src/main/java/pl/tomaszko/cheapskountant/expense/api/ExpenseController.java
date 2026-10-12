package pl.tomaszko.cheapskountant.expense.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.application.DailyExpenseService;
import pl.tomaszko.cheapskountant.expense.application.ExpenseSummaryService;
import pl.tomaszko.cheapskountant.expense.application.StoreExpensesService;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;

@RestController
public class ExpenseController {

    private final StoreExpensesService storeExpensesService;
    private final ExpenseSummaryService expenseSummaryService;
    private final DailyExpenseService dailyExpenseService;
    private final ExpenseCategoryRepository categories;

    public ExpenseController(
            StoreExpensesService storeExpensesService,
            ExpenseSummaryService expenseSummaryService,
            DailyExpenseService dailyExpenseService,
            ExpenseCategoryRepository categories) {
        this.storeExpensesService = storeExpensesService;
        this.expenseSummaryService = expenseSummaryService;
        this.dailyExpenseService = dailyExpenseService;
        this.categories = categories;
    }

    @PostMapping(path = "/api/expense", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExpenseResponse>> store(@RequestBody List<ExpenseSubmission> expenses) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeExpensesService.store(expenses));
    }

    @GetMapping("/api/expense")
    public List<CategoryTotal> summary(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return expenseSummaryService.summarize(from, to);
    }

    @GetMapping("/api/expense/daily")
    public List<DailyExpense> daily(@RequestParam(name = "payment_date", required = false) String paymentDate) {
        return dailyExpenseService.list(paymentDate);
    }

    @GetMapping("/api/expenses")
    public List<CategoryName> names() {
        return categories.findByNameNotOrderByIdAsc(HouseholdCategories.UNKNOWN).stream()
                .map(category -> new CategoryName(category.name()))
                .toList();
    }
}
