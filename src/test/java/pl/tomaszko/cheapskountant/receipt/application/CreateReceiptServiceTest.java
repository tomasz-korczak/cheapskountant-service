package pl.tomaszko.cheapskountant.receipt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptEntity;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptMapper;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptRepository;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@ExtendWith(MockitoExtension.class)
class CreateReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private CreateReceiptService service;

    @BeforeEach
    void setUp() {
        service = new CreateReceiptService(
                new ReceiptCompletenessChecker(),
                receiptRepository,
                new ReceiptMapper(),
                transactionManager);
    }

    @Test
    void savesAReceiptAndReturnsItWithAnAssignedId() throws Exception {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(receiptRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReceiptEntity entity = invocation.getArgument(0);
            Field id = ReceiptEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(entity, 15L);
            return entity;
        });
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        StoredReceiptResponse request = ReceiptFixtures.withoutId(new ReceiptDraft(
                draft.documentType(),
                new ReceiptDraft.Source("page-1.jpg, page-2.jpg", draft.source().rawText()),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()));

        StoredReceiptResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.documentType()).isEqualTo("fiscal_receipt");
        assertThat(response.seller().tradeName()).isEqualTo("Sklep");
        assertThat(response.receipt().number()).isEqualTo("R1");
        assertThat(response.items()).hasSize(1);
        assertThat(response.taxSummary()).hasSize(1);
        assertThat(response.totals().grossAmount()).isEqualTo("4.00");
        assertThat(response.payments()).extracting(payment -> payment.method()).containsExactly("card");
        assertThat(response.source().fileName()).isEqualTo("page-1.jpg, page-2.jpg");
        verify(transactionManager).commit(any());
    }
}
