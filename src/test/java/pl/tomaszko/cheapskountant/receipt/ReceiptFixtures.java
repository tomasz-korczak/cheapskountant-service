package pl.tomaszko.cheapskountant.receipt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

public final class ReceiptFixtures {

    public static final String JSON = """
            {
              "documentType": "fiscal_receipt",
              "source": { "rawText": "PARAGON FISKALNY" },
              "seller": {
                "tradeName": "Sklep",
                "taxId": "1234567890"
              },
              "receipt": {
                "number": "R1",
                "issuedAt": "2024-05-01T12:30:00Z",
                "currency": "PLN"
              },
              "items": [
                {
                  "description": "Milk",
                  "quantity": 1,
                  "unitPrice": "4.00",
                  "total": "4.00",
                  "taxCategory": "A"
                }
              ],
              "taxSummary": [
                {
                  "taxCategory": "A",
                  "taxRate": 23,
                  "taxableSales": "3.25",
                  "taxAmount": "0.75"
                }
              ],
              "totals": {
                "taxAmount": "0.75",
                "grossAmount": "4.00",
                "amountDue": "4.00"
              },
              "payments": [
                { "method": "card", "amount": "4.00" }
              ]
            }
            """;

    private ReceiptFixtures() {
    }

    public static StoredReceiptResponse withoutId(ReceiptDraft draft) {
        return new StoredReceiptResponse(
                null,
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines());
    }

    public static ReceiptDraft validDraft() {
        return new ReceiptDraft(
                "fiscal_receipt",
                new ReceiptDraft.Source(null, "PARAGON FISKALNY"),
                new ReceiptDraft.Seller("Sklep", null, "1234567890", null, null, null),
                new ReceiptDraft.ReceiptInfo("R1", OffsetDateTime.parse("2024-05-01T12:30:00Z"), "PLN", null),
                List.of(new ReceiptDraft.Item("Milk", null, BigDecimal.ONE, null, "4.00", "4.00", "A")),
                List.of(new ReceiptDraft.TaxLine("A", new BigDecimal("23"), "3.25", "0.75")),
                new ReceiptDraft.Totals("0.75", "4.00", "4.00"),
                List.of(new ReceiptDraft.Payment("card", "4.00", null)),
                null,
                null);
    }
}
