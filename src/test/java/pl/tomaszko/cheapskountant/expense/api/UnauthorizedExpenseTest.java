package pl.tomaszko.cheapskountant.expense.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.expense.application.ExpenseSummaryService;
import pl.tomaszko.cheapskountant.expense.application.StoreExpensesService;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;
import pl.tomaszko.cheapskountant.receipt.api.ReceiptErrorHandler;

@WebMvcTest(controllers = ExpenseController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class UnauthorizedExpenseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreExpensesService storeExpensesService;

    @MockitoBean
    private ExpenseSummaryService expenseSummaryService;

    @MockitoBean
    private ExpenseCategoryRepository categories;

    @Test
    void missingKeyIsNotAuthorizedForEveryExpensePath() throws Exception {
        expect(post("/api/expense").contentType(MediaType.APPLICATION_JSON).content("[]"));
        expect(get("/api/expense").param("from", "2026-10-10").param("to", "2026-10-10"));
        expect(get("/api/expenses"));
        verifyNoInteractions(storeExpensesService, expenseSummaryService, categories);
    }

    private void expect(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"))
                .andExpect(jsonPath("$.explanation").value("A valid API key is required."));
    }
}
