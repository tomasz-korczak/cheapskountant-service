package pl.tomaszko.cheapskountant.expense.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.application.ExpenseSubmissionValidator;
import pl.tomaszko.cheapskountant.expense.application.ExpenseSummaryService;
import pl.tomaszko.cheapskountant.expense.application.StoreExpensesService;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseRepository;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.api.ReceiptErrorHandler;

@WebMvcTest(controllers = ExpenseController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class, StoreExpensesService.class, ExpenseSubmissionValidator.class, ExpenseSummaryService.class})
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseRepository expenses;

    @MockitoBean
    private ExpenseCategoryRepository categories;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(categories.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            if (HouseholdCategories.household(name)) {
                return Optional.of(new ExpenseCategoryEntity(name));
            }
            return Optional.empty();
        });
        when(expenses.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesAnExpenseAndReturnsAmountWithScaleTwo() throws Exception {
        mockMvc.perform(post("/api/expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
                        .content("""
                                [{"amount":12.5,"paymentDate":"2026-10-10","category":"Jedzenie","currency":"PLN","description":"chleb"}]
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12.50")))
                .andExpect(jsonPath("$[0].category").value("Jedzenie"))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].paymentDate").value("2026-10-10"))
                .andExpect(jsonPath("$[0].description").value("chleb"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void summaryReturnsOneTotalPerCategoryAndCurrency() throws Exception {
        when(expenses.summarize(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-10")))
                .thenReturn(List.of(
                        new CategoryTotal("Jedzenie", "PLN", new BigDecimal("12.50")),
                        new CategoryTotal("Jedzenie", "EUR", new BigDecimal("3.50"))));

        mockMvc.perform(get("/api/expense")
                        .param("from", "2026-10-10")
                        .param("to", "2026-10-10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("Jedzenie"))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].amount").value(12.50))
                .andExpect(jsonPath("$[1].currency").value("EUR"))
                .andExpect(jsonPath("$[0].description").doesNotExist());
        verify(expenses, never()).saveAll(any());
    }

    @Test
    void rejectsAnInvalidExpenseList() throws Exception {
        expectInvalid("[]");
        expectInvalid("{}");
        expectInvalid(listOf(501));
        expectInvalid("""
                [{"amount":1.00,"paymentDate":"2026-10-10","category":"Jedzenie","currency":"PLN"},
                 {"amount":1.00,"paymentDate":"2026-10-10","category":"Unknown","currency":"PLN"}]
                """);
        expectInvalid("""
                [{"amount":1.00,"paymentDate":"2026-02-31","category":"Jedzenie","currency":"PLN"}]
                """);
        expectInvalid("""
                [{"amount":1.00,"paymentDate":"2026-10-10","category":"Jedzenie","currency":"PLN","description":"   "}]
                """);
        expectInvalid("""
                [{"paymentDate":"2026-10-10","category":"Jedzenie","currency":"PLN"}]
                """);
        verify(expenses, never()).saveAll(any());
    }

    @Test
    void failedSaveIsStorageFailed() throws Exception {
        when(expenses.saveAll(any())).thenThrow(new DataAccessResourceFailureException("down"));

        mockMvc.perform(post("/api/expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
                        .content("""
                                [{"amount":1.00,"paymentDate":"2026-10-10","category":"Jedzenie","currency":"PLN"}]
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.reason").value("storage failed"));
        verify(transactionManager).rollback(any());
    }

    @Test
    void rejectsABadSummaryAndAFailedRead() throws Exception {
        expectSummaryInvalid(null, "2026-10-10");
        expectSummaryInvalid("2026-02-31", "2026-03-01");
        expectSummaryInvalid("2026-10-12", "2026-10-10");
        when(expenses.summarize(any(), any())).thenThrow(new DataAccessResourceFailureException("down"));
        mockMvc.perform(get("/api/expense")
                        .param("from", "2026-10-10")
                        .param("to", "2026-10-10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.reason").value("storage failed"));
    }

    private void expectInvalid(String body) throws Exception {
        mockMvc.perform(post("/api/expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid submission"));
    }

    private void expectSummaryInvalid(String from, String to) throws Exception {
        var request = get("/api/expense").header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key");
        if (from != null) {
            request.param("from", from);
        }
        if (to != null) {
            request.param("to", to);
        }
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid submission"));
    }

    private String listOf(int count) {
        StringBuilder body = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append("{\"amount\":1.00,\"paymentDate\":\"2026-10-10\",\"category\":\"Jedzenie\",\"currency\":\"PLN\"}");
        }
        return body.append(']').toString();
    }
}
