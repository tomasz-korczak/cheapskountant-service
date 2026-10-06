package pl.tomaszko.cheapskountant.receipt.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pl.tomaszko.cheapskountant.receipt.application.TranscribeReceiptService;

@RestController
public class TranscriptionController {

    private final TranscribeReceiptService transcribeReceiptService;

    public TranscriptionController(TranscribeReceiptService transcribeReceiptService) {
        this.transcribeReceiptService = transcribeReceiptService;
    }

    @PostMapping(path = "/api/transcription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredReceiptResponse transcribe(@RequestParam(name = "images", required = false) List<MultipartFile> images) {
        return transcribeReceiptService.transcribe(images);
    }
}
