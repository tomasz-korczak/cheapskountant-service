package pl.tomaszko.cheapskountant.receipt.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptMapper;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptRepository;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@Service
public class CreateReceiptService {

    private final ReceiptCompletenessChecker completenessChecker;
    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final TransactionTemplate transactionTemplate;

    public CreateReceiptService(
            ReceiptCompletenessChecker completenessChecker,
            ReceiptRepository receiptRepository,
            ReceiptMapper receiptMapper,
            PlatformTransactionManager transactionManager) {
        this.completenessChecker = completenessChecker;
        this.receiptRepository = receiptRepository;
        this.receiptMapper = receiptMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public StoredReceiptResponse create(StoredReceiptResponse receipt) {
        if (receipt == null) {
            throw ReceiptFailureException.invalidSubmission("A receipt body is required.");
        }
        ReceiptDraft draft = receiptMapper.toDraft(receipt);
        completenessChecker.check(draft);
        try {
            StoredReceiptResponse response = transactionTemplate.execute(status -> {
                var saved = receiptRepository.saveAndFlush(receiptMapper.toEntity(draft));
                return receiptMapper.toResponse(saved);
            });
            if (response == null) {
                throw ReceiptFailureException.storageFailed(null);
            }
            return response;
        }
        catch (ReceiptFailureException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw ReceiptFailureException.storageFailed(ex);
        }
    }
}
