package pl.tomaszko.cheapskountant.receipt.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;
import pl.tomaszko.cheapskountant.receipt.SubmittedImage;

class ReceiptSubmissionValidatorTest {

    private final ReceiptSubmissionValidator validator = new ReceiptSubmissionValidator();

    @Test
    void rejectsMissingImages() {
        assertInvalid(null);
        assertInvalid(List.of());
    }

    @Test
    void rejectsAnEmptyImage() {
        assertInvalid(List.of(file("empty.jpg", "image/jpeg", 0, new byte[0])));
    }

    @Test
    void rejectsADeclaredTypeOtherThanJpegPngOrWebp() {
        assertInvalid(List.of(file("notes.txt", "text/plain", 4, new byte[] {1, 2, 3, 4})));
    }

    @Test
    void rejectsAnImageOver10Mb() {
        assertInvalid(List.of(file("huge.jpg", "image/jpeg", ReceiptSubmissionValidator.MAX_IMAGE_BYTES + 1, new byte[] {1})));
    }

    @Test
    void rejectsASixthImage() {
        assertInvalid(List.of(
                file("1.jpg", "image/jpeg", 1, new byte[] {1}),
                file("2.jpg", "image/jpeg", 1, new byte[] {1}),
                file("3.jpg", "image/jpeg", 1, new byte[] {1}),
                file("4.jpg", "image/jpeg", 1, new byte[] {1}),
                file("5.jpg", "image/jpeg", 1, new byte[] {1}),
                file("6.jpg", "image/jpeg", 1, new byte[] {1})));
    }

    @Test
    void acceptsAMissingMediaTypeAndKeepsOrder() {
        List<SubmittedImage> images = validator.prepare(List.of(
                file("first.png", "image/png", 1, new byte[] {9}),
                file("second.jpg", null, 2, new byte[] {8, 7})));

        assertThat(images).hasSize(2);
        assertThat(images.get(0).fileName()).isEqualTo("first.png");
        assertThat(images.get(0).mediaType()).isEqualTo("image/png");
        assertThat(images.get(1).mediaType()).isNull();
        assertThat(images.get(1).bytes()).containsExactly(8, 7);
    }

    private void assertInvalid(List<MultipartFile> files) {
        assertThatThrownBy(() -> validator.prepare(files))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INVALID_SUBMISSION);
    }

    private MultipartFile file(String name, String contentType, long size, byte[] content) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "images";
            }

            @Override
            public String getOriginalFilename() {
                return name;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return size == 0;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public byte[] getBytes() {
                return content;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new IOException("unused");
            }
        };
    }
}
