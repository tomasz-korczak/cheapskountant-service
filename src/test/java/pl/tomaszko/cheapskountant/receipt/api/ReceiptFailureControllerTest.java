package pl.tomaszko.cheapskountant.receipt.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.application.CreateReceiptService;

@WebMvcTest(controllers = ReceiptController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class ReceiptFailureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateReceiptService createReceiptService;

    @Test
    void incompleteIs422() throws Exception {
        when(createReceiptService.create(any())).thenThrow(ReceiptFailureException.incomplete("The seller tax identifier is missing."));
        expect(422, "incomplete");
    }

    @Test
    void unreadableBodyIs422() throws Exception {
        when(createReceiptService.create(any())).thenThrow(ReceiptFailureException.unreadable("The receipt is not one fiscal receipt."));
        expect(422, "unreadable");
    }

    @Test
    void storageFailureIs500() throws Exception {
        when(createReceiptService.create(any())).thenThrow(ReceiptFailureException.storageFailed(new IllegalStateException("db")));
        expect(500, "storage failed");
    }

    private void expect(int status, String reason) throws Exception {
        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReceiptFixtures.JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }
}
