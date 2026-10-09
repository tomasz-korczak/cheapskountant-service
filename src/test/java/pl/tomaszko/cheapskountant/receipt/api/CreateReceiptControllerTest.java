package pl.tomaszko.cheapskountant.receipt.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.application.CreateReceiptService;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@WebMvcTest(controllers = ReceiptController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class CreateReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateReceiptService createReceiptService;

    @Test
    void jsonBodyReturnsTheStoredReceipt() throws Exception {
        org.mockito.Mockito.when(createReceiptService.create(any())).thenReturn(stored());

        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReceiptFixtures.JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.documentType").value("fiscal_receipt"))
                .andExpect(jsonPath("$.seller.tradeName").value("Sklep"))
                .andExpect(jsonPath("$.seller.taxId").value("1234567890"))
                .andExpect(jsonPath("$.receipt.number").value("R1"))
                .andExpect(jsonPath("$.items[0].description").value("Milk"))
                .andExpect(jsonPath("$.items[0].category").value("Unknown"))
                .andExpect(jsonPath("$.taxSummary[0].taxCategory").value("A"))
                .andExpect(jsonPath("$.totals.grossAmount").value("4.00"))
                .andExpect(jsonPath("$.payments[0].method").value("card"));

        verify(createReceiptService).create(org.mockito.ArgumentMatchers.argThat(body ->
                body.id() == null
                        && "Sklep".equals(body.seller().tradeName())
                        && "R1".equals(body.receipt().number())
                        && body.discountSummary() == null
                        && body.items().get(0).discount() == null));
    }

    @Test
    void discountFieldsAreAcceptedAndReturned() throws Exception {
        org.mockito.Mockito.when(createReceiptService.create(any())).thenReturn(discounted());

        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReceiptFixtures.DISCOUNTED_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[1].description").value("ChipsyLay soveB110g"))
                .andExpect(jsonPath("$.items[1].total").value("26.07"))
                .andExpect(jsonPath("$.items[1].discount.description").value("OPUST"))
                .andExpect(jsonPath("$.items[1].discount.total").value("-8.69"))
                .andExpect(jsonPath("$.items[1].discount.quantity").doesNotExist())
                .andExpect(jsonPath("$.items[2].itemType").value("packaging"))
                .andExpect(jsonPath("$.discountSummary.description").value("OPUSTY ŁĄCZNIE"))
                .andExpect(jsonPath("$.discountSummary.total").value("-8.69"));

        verify(createReceiptService).create(org.mockito.ArgumentMatchers.argThat(body ->
                body.items().size() == 3
                        && body.items().get(0).discount() == null
                        && "OPUST".equals(body.items().get(1).discount().description())
                        && "-8.69".equals(body.items().get(1).discount().total())
                        && "26.07".equals(body.items().get(1).total())
                        && "OPUSTY ŁĄCZNIE".equals(body.discountSummary().description())
                        && "-8.69".equals(body.discountSummary().total())));
    }

    @Test
    void missingBodyIsInvalidSubmission() throws Exception {
        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid submission"))
                .andExpect(jsonPath("$.explanation").value("A receipt body is required."));
    }

    private StoredReceiptResponse stored() {
        return new StoredReceiptResponse(
                9L,
                "fiscal_receipt",
                null,
                new ReceiptDraft.Seller("Sklep", null, "1234567890", null, null, null),
                new ReceiptDraft.ReceiptInfo("R1", OffsetDateTime.parse("2024-05-01T12:30:00Z"), "PLN", null),
                List.of(new ReceiptDraft.Item("Milk", null, BigDecimal.ONE, null, "4.00", "4.00", "A", "Unknown", null)),
                null,
                List.of(new ReceiptDraft.TaxLine("A", new BigDecimal("23"), "3.25", "0.75")),
                new ReceiptDraft.Totals("0.75", "4.00", "4.00"),
                List.of(new ReceiptDraft.Payment("card", "4.00", null)),
                null,
                null);
    }

    private StoredReceiptResponse discounted() {
        ReceiptDraft draft = ReceiptFixtures.discountedDraft();
        return new StoredReceiptResponse(
                9L,
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.discountSummary(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines());
    }
}
