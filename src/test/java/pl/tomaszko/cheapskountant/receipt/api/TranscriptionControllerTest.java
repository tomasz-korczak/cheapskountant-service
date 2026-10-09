package pl.tomaszko.cheapskountant.receipt.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.application.TranscribeReceiptService;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@WebMvcTest(controllers = TranscriptionController.class)
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscribeReceiptService transcribeReceiptService;

    @Test
    void oneImageReturnsTheTranscribedReceiptWithoutAnId() throws Exception {
        when(transcribeReceiptService.transcribe(any())).thenReturn(transcribed());

        mockMvc.perform(multipart("/api/transcription")
                        .file(image("page.jpg"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.documentType").value("fiscal_receipt"))
                .andExpect(jsonPath("$.seller.tradeName").value("Sklep"))
                .andExpect(jsonPath("$.items[0].description").value("Milk"))
                .andExpect(jsonPath("$.items[0].category").value("Unknown"))
                .andExpect(jsonPath("$.taxSummary[0].taxCategory").value("A"))
                .andExpect(jsonPath("$.totals.grossAmount").value("4.00"))
                .andExpect(jsonPath("$.payments[0].method").value("card"));
    }

    @Test
    void twoToFiveImagesAreAcceptedInOrder() throws Exception {
        when(transcribeReceiptService.transcribe(any())).thenReturn(transcribed());

        mockMvc.perform(multipart("/api/transcription")
                        .file(image("page-1.jpg"))
                        .file(image("page-2.jpg"))
                        .file(image("page-3.jpg"))
                        .file(image("page-4.jpg"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(transcribeReceiptService).transcribe(org.mockito.ArgumentMatchers.<List<MultipartFile>>argThat(files ->
                files.size() == 4
                        && "page-1.jpg".equals(files.get(0).getOriginalFilename())
                        && "page-4.jpg".equals(files.get(3).getOriginalFilename())));
    }

    @Test
    void unreadableIs422() throws Exception {
        when(transcribeReceiptService.transcribe(any()))
                .thenThrow(ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt."));
        expect(422, "unreadable");
    }

    @Test
    void incompleteIs422() throws Exception {
        when(transcribeReceiptService.transcribe(any()))
                .thenThrow(ReceiptFailureException.incomplete("The seller tax identifier is missing."));
        expect(422, "incomplete");
    }

    @Test
    void providerFailureIs503() throws Exception {
        when(transcribeReceiptService.transcribe(any()))
                .thenThrow(ReceiptFailureException.transcriptionUnavailable(new IllegalStateException("down")));
        expect(503, "transcription unavailable");
    }

    @Test
    void timeoutIs504() throws Exception {
        when(transcribeReceiptService.transcribe(any()))
                .thenThrow(ReceiptFailureException.transcriptionTimeout(new IllegalStateException("slow")));
        expect(504, "transcription unavailable");
    }

    private void expect(int status, String reason) throws Exception {
        mockMvc.perform(multipart("/api/transcription")
                        .file(image("page.jpg"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }

    private StoredReceiptResponse transcribed() {
        return new StoredReceiptResponse(
                null,
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

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("images", name, MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
    }
}
