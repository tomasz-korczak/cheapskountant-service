package pl.tomaszko.cheapskountant.receipt.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoredReceiptResponse(
        Long id,
        String documentType,
        ReceiptDraft.Source source,
        ReceiptDraft.Seller seller,
        ReceiptDraft.ReceiptInfo receipt,
        List<ReceiptDraft.Item> items,
        ReceiptDraft.Discount discountSummary,
        List<ReceiptDraft.TaxLine> taxSummary,
        ReceiptDraft.Totals totals,
        List<ReceiptDraft.Payment> payments,
        ReceiptDraft.FiscalData fiscalData,
        List<String> unparsedLines) {
}
