package pl.tomaszko.cheapskountant.receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum ReceiptFailureReason {
    INVALID_SUBMISSION("invalid submission"),
    NOT_AUTHORIZED("not authorized"),
    UNREADABLE("unreadable"),
    INCOMPLETE("incomplete"),
    TRANSCRIPTION_UNAVAILABLE("transcription unavailable"),
    STORAGE_FAILED("storage failed");

    private final String wireValue;
}
