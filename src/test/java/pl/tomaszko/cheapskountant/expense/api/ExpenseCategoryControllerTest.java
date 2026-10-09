package pl.tomaszko.cheapskountant.expense.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.expense.application.ExpenseSummaryService;
import pl.tomaszko.cheapskountant.expense.application.StoreExpensesService;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryEntity;
import pl.tomaszko.cheapskountant.expense.persistence.ExpenseCategoryRepository;
import pl.tomaszko.cheapskountant.receipt.api.ReceiptErrorHandler;

@WebMvcTest(controllers = ExpenseController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class ExpenseCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreExpensesService storeExpensesService;

    @MockitoBean
    private ExpenseSummaryService expenseSummaryService;

    @MockitoBean
    private ExpenseCategoryRepository categories;

    @Test
    void returnsHouseholdNamesInListedOrderWithoutUnknown() throws Exception {
        when(categories.findByNameNotOrderByIdAsc(HouseholdCategories.UNKNOWN))
                .thenReturn(HouseholdCategories.NAMES.stream().map(ExpenseCategoryEntity::new).toList());

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(34))
                .andExpect(jsonPath("$[0].name").value("Jedzenie"))
                .andExpect(jsonPath("$[33].name").value("Fermentacja alkoholowa"))
                .andExpect(jsonPath("$[?(@.name == 'Unknown')]").doesNotExist());
    }

    @Test
    void missingKeyIsNotAuthorized() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"))
                .andExpect(jsonPath("$.explanation").value("A valid API key is required."));
    }
}
