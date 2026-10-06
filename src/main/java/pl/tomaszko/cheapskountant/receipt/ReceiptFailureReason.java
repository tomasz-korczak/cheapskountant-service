package pl.tomaszko.cheapskountant.receipt;

public enum ReceiptFailureReason {
    INVALID_SUBMISSION("invalid submission"),
    NOT_AUTHORIZED("not authorized"),
    UNREADABLE("unreadable"),
    INCOMPLETE("incomplete"),
    TRANSCRIPTION_UNAVAILABLE("transcription unavailable"),
    STORAGE_FAILED("storage failed");

    private final String wireValue;

    ReceiptFailureReason(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
