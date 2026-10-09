package pl.tomaszko.cheapskountant.receipt.transcription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiptDraft(
        String documentType,
        Source source,
        Seller seller,
        ReceiptInfo receipt,
        List<Item> items,
        Discount discountSummary,
        List<TaxLine> taxSummary,
        Totals totals,
        List<Payment> payments,
        FiscalData fiscalData,
        List<String> unparsedLines) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Source(String fileName, String rawText) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Seller(
            String tradeName,
            String legalName,
            String taxId,
            String bdoNumber,
            Address businessAddress,
            Address registeredAddress) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Address(String street, String postalCode, String city, String countryCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReceiptInfo(String number, OffsetDateTime issuedAt, String currency, String orderNumber) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            String description,
            String itemType,
            BigDecimal quantity,
            String unit,
            String unitPrice,
            String total,
            String taxCategory,
            String category,
            Discount discount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Discount(String description, String total) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaxLine(String taxCategory, BigDecimal taxRate, String taxableSales, String taxAmount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Totals(String taxAmount, String grossAmount, String amountDue) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payment(String method, String amount, String transactionId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FiscalData(
            String cashRegisterCode,
            String cashierCode,
            String fiscalDeviceNumber,
            String verificationHash,
            String rawIdentificationLine) {
    }
}
