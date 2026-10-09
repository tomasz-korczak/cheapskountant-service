package pl.tomaszko.cheapskountant.receipt;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ReceiptFailureException extends RuntimeException {

    private final ReceiptFailureReason reason;
    private final boolean timeout;

    private ReceiptFailureException(ReceiptFailureReason reason, String explanation, boolean timeout, Throwable cause) {
        super(explanation, cause);
        this.reason = reason;
        this.timeout = timeout;
    }

    public String explanation() {
        return getMessage();
    }

    public static ReceiptFailureException invalidSubmission(String explanation) {
        return new ReceiptFailureException(ReceiptFailureReason.INVALID_SUBMISSION, explanation, false, null);
    }

    public static ReceiptFailureException notAuthorized(String explanation) {
        return new ReceiptFailureException(ReceiptFailureReason.NOT_AUTHORIZED, explanation, false, null);
    }

    public static ReceiptFailureException unreadable(String explanation) {
        return new ReceiptFailureException(ReceiptFailureReason.UNREADABLE, explanation, false, null);
    }

    public static ReceiptFailureException unreadable(String explanation, Throwable cause) {
        return new ReceiptFailureException(ReceiptFailureReason.UNREADABLE, explanation, false, cause);
    }

    public static ReceiptFailureException incomplete(String explanation) {
        return new ReceiptFailureException(ReceiptFailureReason.INCOMPLETE, explanation, false, null);
    }

    public static ReceiptFailureException transcriptionUnavailable(Throwable cause) {
        return new ReceiptFailureException(
                ReceiptFailureReason.TRANSCRIPTION_UNAVAILABLE,
                "The transcription service did not respond.",
                false,
                cause);
    }

    public static ReceiptFailureException transcriptionTimeout(Throwable cause) {
        return new ReceiptFailureException(
                ReceiptFailureReason.TRANSCRIPTION_UNAVAILABLE,
                "Transcription did not finish in time.",
                true,
                cause);
    }

    public static ReceiptFailureException storageFailed(Throwable cause) {
        return storageFailed("The receipt could not be saved.", cause);
    }

    public static ReceiptFailureException storageFailed(String explanation, Throwable cause) {
        return new ReceiptFailureException(ReceiptFailureReason.STORAGE_FAILED, explanation, false, cause);
    }
}
