package pl.tomaszko.cheapskountant.receipt;

public record SubmittedImage(String fileName, String mediaType, byte[] bytes) {
}
