package pl.tomaszko.cheapskountant.receipt.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.SubmittedImage;

@Component
public class ReceiptSubmissionValidator {

    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    static final int MAX_IMAGES = 5;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public List<SubmittedImage> prepare(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw ReceiptFailureException.invalidSubmission("At least one receipt image is required.");
        }
        if (files.size() > MAX_IMAGES) {
            throw ReceiptFailureException.invalidSubmission("At most five images are allowed.");
        }
        List<SubmittedImage> images = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw ReceiptFailureException.invalidSubmission("An image file is empty.");
            }
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw ReceiptFailureException.invalidSubmission("An image exceeds 10 MB.");
            }
            String mediaType = declaredType(file.getContentType());
            if (mediaType != null && !ALLOWED_TYPES.contains(mediaType)) {
                throw ReceiptFailureException.invalidSubmission("An image type is not supported.");
            }
            try {
                images.add(new SubmittedImage(file.getOriginalFilename(), mediaType, file.getBytes()));
            }
            catch (IOException ex) {
                throw ReceiptFailureException.invalidSubmission("An image could not be read.");
            }
        }
        return images;
    }

    private String declaredType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(type)) {
            return "image/jpeg";
        }
        return type;
    }
}
