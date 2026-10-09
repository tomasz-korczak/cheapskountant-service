package pl.tomaszko.cheapskountant.receipt.application;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pl.tomaszko.cheapskountant.receipt.SubmittedImage;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.persistence.ReceiptMapper;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptTranscriptionService;

@Service
@RequiredArgsConstructor
public class TranscribeReceiptService {

    private final ReceiptSubmissionValidator submissionValidator;
    private final ReceiptTranscriptionService transcriptionService;
    private final ReceiptMapper receiptMapper;

    public StoredReceiptResponse transcribe(List<MultipartFile> files) {
        List<SubmittedImage> images = submissionValidator.prepare(files);
        ReceiptDraft draft = transcriptionService.transcribe(images);
        return receiptMapper.toTranscribedResponse(draft, images);
    }
}
