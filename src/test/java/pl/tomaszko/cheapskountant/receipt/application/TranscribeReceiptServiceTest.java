package pl.tomaszko.cheapskountant.receipt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptMapper;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptTranscriptionService;

@ExtendWith(MockitoExtension.class)
class TranscribeReceiptServiceTest {

    @Mock
    private ReceiptTranscriptionService transcriptionService;

    private TranscribeReceiptService service;

    @BeforeEach
    void setUp() {
        service = new TranscribeReceiptService(
                new ReceiptSubmissionValidator(),
                transcriptionService,
                new ReceiptMapper());
    }

    @Test
    void returnsTheDraftWithoutAnIdAndKeepsUploadFileNames() {
        when(transcriptionService.transcribe(any())).thenReturn(ReceiptFixtures.validDraft());
        MockMultipartFile first = new MockMultipartFile("images", "page-1.jpg", "image/jpeg", new byte[] {1});
        MockMultipartFile second = new MockMultipartFile("images", "page-2.jpg", "image/jpeg", new byte[] {2});

        StoredReceiptResponse response = service.transcribe(List.of(first, second));

        assertThat(response.id()).isNull();
        assertThat(response.documentType()).isEqualTo("fiscal_receipt");
        assertThat(response.seller().tradeName()).isEqualTo("Sklep");
        assertThat(response.source().fileName()).isEqualTo("page-1.jpg, page-2.jpg");
        assertThat(response.source().rawText()).isEqualTo("PARAGON FISKALNY");
    }

    @Test
    void invalidImagesDoNotCallTheModel() {
        assertThatThrownBy(() -> service.transcribe(List.of()))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
        verify(transcriptionService, never()).transcribe(any());
    }
}
