package pl.tomaszko.cheapskountant.receipt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptEntity;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptMapper;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptRepository;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@ExtendWith(MockitoExtension.class)
class CreateReceiptFailureTest {

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
    void missingBodyIsInvalidAndNothingIsSaved() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidLineItemIsIncompleteAndNothingIsSaved() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                List.of(new ReceiptDraft.Item("Milk", null, BigDecimal.ONE, null, "4.00", null, "A", null)),
                draft.discountSummary(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void unsupportedPaymentMethodIsIncomplete() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.discountSummary(),
                draft.taxSummary(),
                draft.totals(),
                List.of(new ReceiptDraft.Payment("bitcoin", "4.00", null)),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void malformedTaxIdIsIncomplete() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                new ReceiptDraft.Seller("Sklep", null, "ABC", null, null, null),
                draft.receipt(),
                draft.items(),
                draft.discountSummary(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
    }

    @Test
    void nonNegativeItemDiscountIsIncomplete() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                List.of(new ReceiptDraft.Item(
                        "Milk",
                        null,
                        BigDecimal.ONE,
                        null,
                        "4.00",
                        "4.00",
                        "A",
                        new ReceiptDraft.Discount("OPUST", "1.00"))),
                null,
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void blankDiscountSummaryIsIncomplete() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                new ReceiptDraft.Discount("  ", "-8.69"),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void zeroDiscountSummaryIsIncomplete() {
        ReceiptDraft draft = ReceiptFixtures.validDraft();
        assertFailure(new ReceiptDraft(
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                new ReceiptDraft.Discount("OPUSTY ŁĄCZNIE", "-0.00"),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines()), ReceiptFailureReason.INCOMPLETE);
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void storageFailureRollsBack() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(receiptRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("down"));

        assertFailure(ReceiptFixtures.validDraft(), ReceiptFailureReason.STORAGE_FAILED);
        verify(transactionManager).rollback(any());
    }

    @Test
    void suppliedIdIsIgnored() throws Exception {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(receiptRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReceiptEntity entity = invocation.getArgument(0);
            var id = ReceiptEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            assertThat(id.get(entity)).isNull();
            id.set(entity, 4L);
            return entity;
        });

        StoredReceiptResponse request = ReceiptFixtures.withoutId(ReceiptFixtures.validDraft());
        StoredReceiptResponse withClientId = new StoredReceiptResponse(
                99L,
                request.documentType(),
                request.source(),
                request.seller(),
                request.receipt(),
                request.items(),
                request.discountSummary(),
                request.taxSummary(),
                request.totals(),
                request.payments(),
                request.fiscalData(),
                request.unparsedLines());

        assertThat(service.create(withClientId).id()).isEqualTo(4L);
    }

    private void assertFailure(ReceiptDraft draft, ReceiptFailureReason reason) {
        assertThatThrownBy(() -> service.create(ReceiptFixtures.withoutId(draft)))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(reason);
    }
}
