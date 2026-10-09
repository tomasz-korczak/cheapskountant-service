package pl.tomaszko.cheapskountant.receipt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
                  "taxCategory": "A",
                  "category": "Unknown"
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

    public static final String DISCOUNTED_JSON = """
            {
              "documentType": "fiscal_receipt",
              "seller": {
                "tradeName": "BIEDRONKA",
                "taxId": "7791011327"
              },
              "receipt": {
                "number": "499752",
                "issuedAt": "2026-09-29T20:02:00Z",
                "currency": "PLN"
              },
              "items": [
                {
                  "description": "NapLiptIcTeMIX1,5L",
                  "quantity": 1,
                  "unitPrice": "5.99",
                  "total": "5.99",
                  "taxCategory": "C",
                  "category": "Unknown"
                },
                {
                  "description": "ChipsyLay soveB110g",
                  "quantity": 3,
                  "unitPrice": "8.69",
                  "total": "26.07",
                  "taxCategory": "C",
                  "category": "Unknown",
                  "discount": {
                    "description": "OPUST",
                    "total": "-8.69"
                  }
                },
                {
                  "description": "But Plastik kaucja",
                  "itemType": "packaging",
                  "quantity": 1,
                  "unitPrice": "0.50",
                  "total": "0.50",
                  "taxCategory": "C",
                  "category": "Unknown"
                }
              ],
              "discountSummary": {
                "description": "OPUSTY ŁĄCZNIE",
                "total": "-8.69"
              },
              "taxSummary": [
                {
                  "taxCategory": "C",
                  "category": "Unknown",
                  "taxRate": 23,
                  "taxableSales": "23.37",
                  "taxAmount": "1.11"
                }
              ],
              "totals": {
                "taxAmount": "1.11",
                "grossAmount": "23.37",
                "amountDue": "23.87"
              },
              "payments": [
                { "method": "card", "amount": "23.87" }
              ]
            }
            """;

    public static StoredReceiptResponse withoutId(ReceiptDraft draft) {
        return new StoredReceiptResponse(
                null,
                draft.documentType(),
                draft.source(),
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.discountSummary(),
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
                List.of(new ReceiptDraft.Item("Milk", null, BigDecimal.ONE, null, "4.00", "4.00", "A", "Unknown", null)),
                null,
                List.of(new ReceiptDraft.TaxLine("A", new BigDecimal("23"), "3.25", "0.75")),
                new ReceiptDraft.Totals("0.75", "4.00", "4.00"),
                List.of(new ReceiptDraft.Payment("card", "4.00", null)),
                null,
                null);
    }

    public static ReceiptDraft discountedDraft() {
        return new ReceiptDraft(
                "fiscal_receipt",
                null,
                new ReceiptDraft.Seller("BIEDRONKA", null, "7791011327", null, null, null),
                new ReceiptDraft.ReceiptInfo("499752", OffsetDateTime.parse("2026-09-29T20:02:00Z"), "PLN", null),
                List.of(
                        new ReceiptDraft.Item("NapLiptIcTeMIX1,5L", null, BigDecimal.ONE, null, "5.99", "5.99", "C", "Unknown", null),
                        new ReceiptDraft.Item(
                                "ChipsyLay soveB110g",
                                null,
                                new BigDecimal("3"),
                                null,
                                "8.69",
                                "26.07",
                                "C",
                                "Unknown",
                                new ReceiptDraft.Discount("OPUST", "-8.69")),
                        new ReceiptDraft.Item("But Plastik kaucja", "packaging", BigDecimal.ONE, null, "0.50", "0.50", "C", "Unknown", null)),
                new ReceiptDraft.Discount("OPUSTY ŁĄCZNIE", "-8.69"),
                List.of(new ReceiptDraft.TaxLine("C", new BigDecimal("23"), "23.37", "1.11")),
                new ReceiptDraft.Totals("1.11", "23.37", "23.87"),
                List.of(new ReceiptDraft.Payment("card", "23.87", null)),
                null,
                null);
    }
}
