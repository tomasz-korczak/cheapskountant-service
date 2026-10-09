package pl.tomaszko.cheapskountant.receipt.api;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pl.tomaszko.cheapskountant.receipt.application.CreateReceiptService;

@RestController
@RequiredArgsConstructor
public class ReceiptController {

    private final CreateReceiptService createReceiptService;

    @PostMapping(path = "/api/receipt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StoredReceiptResponse> create(@RequestBody StoredReceiptResponse receipt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createReceiptService.create(receipt));
    }
}
