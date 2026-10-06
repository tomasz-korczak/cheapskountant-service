package pl.tomaszko.cheapskountant.receipt.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszko.cheapskountant.config.SecurityConfig;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.application.CreateReceiptService;
import pl.tomaszko.cheapskountant.receipt.application.TranscribeReceiptService;

@WebMvcTest(controllers = {ReceiptController.class, TranscriptionController.class})
@Import({ReceiptErrorHandler.class, SecurityConfig.class})
class UnauthorizedReceiptTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateReceiptService createReceiptService;

    @MockitoBean
    private TranscribeReceiptService transcribeReceiptService;

    @Test
    void missingTokenOnReceiptIsNotAuthorized() throws Exception {
        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReceiptFixtures.JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"))
                .andExpect(jsonPath("$.explanation").value("A valid API key is required."));
        verifyNoInteractions(createReceiptService);
    }

    @Test
    void wrongTokenOnReceiptIsNotAuthorized() throws Exception {
        mockMvc.perform(post("/api/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReceiptFixtures.JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"));
        verifyNoInteractions(createReceiptService);
    }

    @Test
    void missingTokenOnTranscriptionIsNotAuthorized() throws Exception {
        mockMvc.perform(multipart("/api/transcription")
                        .file(new MockMultipartFile("images", "page.jpg", "image/jpeg", new byte[] {1})))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"));
        verifyNoInteractions(transcribeReceiptService);
    }

    @Test
    void wrongTokenOnTranscriptionIsNotAuthorized() throws Exception {
        mockMvc.perform(multipart("/api/transcription")
                        .file(new MockMultipartFile("images", "page.jpg", "image/jpeg", new byte[] {1}))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("not authorized"));
        verifyNoInteractions(transcribeReceiptService);
    }
}
