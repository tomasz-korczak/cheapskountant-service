package pl.tomaszko.cheapskountant.receipt.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;

@RestControllerAdvice
public class ReceiptErrorHandler {

    @ExceptionHandler(ReceiptFailureException.class)
    public ResponseEntity<ReceiptFailure> handle(ReceiptFailureException exception) {
        return ResponseEntity.status(status(exception)).body(new ReceiptFailure(exception.reason().wireValue(), exception.explanation()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ReceiptFailure> handleUploadTooLarge() {
        return invalidSubmission("An image exceeds 10 MB.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReceiptFailure> handleUnreadableBody(HttpServletRequest request) {
        if (isReceipt(request)) {
            return invalidSubmission("A receipt body is required.");
        }
        return invalidSubmission("The request body could not be read.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ReceiptFailure> handleUnsupportedMediaType(HttpServletRequest request) {
        if (isTranscription(request)) {
            return invalidSubmission("Images must be sent as multipart form data.");
        }
        return invalidSubmission("A receipt must be sent as JSON.");
    }

    private ResponseEntity<ReceiptFailure> invalidSubmission(String explanation) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReceiptFailure(ReceiptFailureReason.INVALID_SUBMISSION.wireValue(), explanation));
    }

    private boolean isReceipt(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.endsWith("/api/receipt");
    }

    private boolean isTranscription(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.endsWith("/api/transcription");
    }

    private HttpStatus status(ReceiptFailureException exception) {
        return switch (exception.reason()) {
            case INVALID_SUBMISSION -> HttpStatus.BAD_REQUEST;
            case NOT_AUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case UNREADABLE, INCOMPLETE -> HttpStatus.UNPROCESSABLE_CONTENT;
            case TRANSCRIPTION_UNAVAILABLE -> exception.timeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
            case STORAGE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
