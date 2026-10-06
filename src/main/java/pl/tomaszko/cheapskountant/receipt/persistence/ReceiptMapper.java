package pl.tomaszko.cheapskountant.receipt.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.tomaszko.cheapskountant.receipt.SubmittedImage;
import pl.tomaszko.cheapskountant.receipt.api.StoredReceiptResponse;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@Component
public class ReceiptMapper {

    public ReceiptEntity toEntity(ReceiptDraft draft) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.documentType = draft.documentType();
        if (draft.source() != null) {
            receipt.sourceFileName = blankToNull(draft.source().fileName());
            receipt.sourceRawText = blankToNull(draft.source().rawText());
        }

        SellerEntity seller = new SellerEntity();
        seller.receipt = receipt;
        seller.tradeName = draft.seller().tradeName();
        seller.legalName = blankToNull(draft.seller().legalName());
        seller.taxId = draft.seller().taxId();
        seller.bdoNumber = blankToNull(draft.seller().bdoNumber());
        addAddress(seller, AddressEntity.Role.BUSINESS, draft.seller().businessAddress());
        addAddress(seller, AddressEntity.Role.REGISTERED, draft.seller().registeredAddress());
        receipt.seller = seller;

        ReceiptHeaderEntity header = new ReceiptHeaderEntity();
        header.receipt = receipt;
        header.receiptNumber = draft.receipt().number();
        header.issuedAt = draft.receipt().issuedAt().toLocalDateTime();
        header.currency = draft.receipt().currency();
        header.orderNumber = blankToNull(draft.receipt().orderNumber());
        receipt.header = header;

        int itemNo = 1;
        for (ReceiptDraft.Item item : draft.items()) {
            ReceiptItemEntity row = new ReceiptItemEntity();
            row.receipt = receipt;
            row.lineNo = itemNo++;
            row.description = item.description();
            row.itemType = blankToNull(item.itemType());
            row.quantity = item.quantity();
            row.unit = blankToNull(item.unit());
            row.unitPrice = money(item.unitPrice());
            row.lineTotal = money(item.total());
            row.taxCategory = item.taxCategory();
            receipt.items.add(row);
        }

        int taxNo = 1;
        for (ReceiptDraft.TaxLine line : draft.taxSummary()) {
            TaxSummaryEntity row = new TaxSummaryEntity();
            row.receipt = receipt;
            row.lineNo = taxNo++;
            row.taxCategory = line.taxCategory();
            row.taxRate = line.taxRate();
            row.taxableSales = money(line.taxableSales());
            row.taxAmount = money(line.taxAmount());
            receipt.taxSummaries.add(row);
        }

        ReceiptTotalsEntity totals = new ReceiptTotalsEntity();
        totals.receipt = receipt;
        totals.taxAmount = money(draft.totals().taxAmount());
        totals.grossAmount = money(draft.totals().grossAmount());
        totals.amountDue = money(draft.totals().amountDue());
        receipt.totals = totals;

        int paymentNo = 1;
        for (ReceiptDraft.Payment payment : draft.payments()) {
            PaymentEntity row = new PaymentEntity();
            row.receipt = receipt;
            row.lineNo = paymentNo++;
            row.method = payment.method();
            row.amount = money(payment.amount());
            row.transactionId = blankToNull(payment.transactionId());
            receipt.payments.add(row);
        }

        if (draft.fiscalData() != null) {
            FiscalDataEntity fiscal = new FiscalDataEntity();
            fiscal.receipt = receipt;
            fiscal.cashRegisterCode = blankToNull(draft.fiscalData().cashRegisterCode());
            fiscal.cashierCode = blankToNull(draft.fiscalData().cashierCode());
            fiscal.fiscalDeviceNumber = blankToNull(draft.fiscalData().fiscalDeviceNumber());
            fiscal.verificationHash = blankToNull(draft.fiscalData().verificationHash());
            fiscal.rawIdentificationLine = blankToNull(draft.fiscalData().rawIdentificationLine());
            receipt.fiscalData = fiscal;
        }

        if (draft.unparsedLines() != null) {
            int lineNo = 1;
            for (String line : draft.unparsedLines()) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                UnparsedLineEntity row = new UnparsedLineEntity();
                row.receipt = receipt;
                row.lineNo = lineNo++;
                row.lineText = line;
                receipt.unparsedLines.add(row);
            }
        }
        return receipt;
    }

    public ReceiptDraft toDraft(StoredReceiptResponse receipt) {
        return new ReceiptDraft(
                receipt.documentType(),
                receipt.source(),
                receipt.seller(),
                receipt.receipt(),
                receipt.items(),
                receipt.taxSummary(),
                receipt.totals(),
                receipt.payments(),
                receipt.fiscalData(),
                receipt.unparsedLines());
    }

    public StoredReceiptResponse toTranscribedResponse(ReceiptDraft draft, List<SubmittedImage> images) {
        String fileName = joinedFileNames(images);
        String rawText = draft.source() == null ? null : blankToNull(draft.source().rawText());
        ReceiptDraft.Source source = fileName == null && rawText == null ? null : new ReceiptDraft.Source(fileName, rawText);
        return new StoredReceiptResponse(
                null,
                draft.documentType(),
                source,
                draft.seller(),
                draft.receipt(),
                draft.items(),
                draft.taxSummary(),
                draft.totals(),
                draft.payments(),
                draft.fiscalData(),
                draft.unparsedLines());
    }

    public StoredReceiptResponse toResponse(ReceiptEntity receipt) {
        return new StoredReceiptResponse(
                receipt.id,
                receipt.documentType,
                source(receipt),
                seller(receipt.seller),
                header(receipt.header),
                items(receipt.items),
                taxLines(receipt.taxSummaries),
                totals(receipt.totals),
                payments(receipt.payments),
                fiscal(receipt.fiscalData),
                unparsed(receipt.unparsedLines));
    }

    private void addAddress(SellerEntity seller, AddressEntity.Role role, ReceiptDraft.Address address) {
        if (address == null) {
            return;
        }
        AddressEntity row = new AddressEntity();
        row.seller = seller;
        row.role = role;
        row.street = address.street();
        row.postalCode = address.postalCode();
        row.city = address.city();
        row.countryCode = address.countryCode();
        seller.addresses.add(row);
    }

    private ReceiptDraft.Source source(ReceiptEntity receipt) {
        if (receipt.sourceFileName == null && receipt.sourceRawText == null) {
            return null;
        }
        return new ReceiptDraft.Source(receipt.sourceFileName, receipt.sourceRawText);
    }

    private ReceiptDraft.Seller seller(SellerEntity seller) {
        return new ReceiptDraft.Seller(
                seller.tradeName,
                seller.legalName,
                seller.taxId,
                seller.bdoNumber,
                address(seller, AddressEntity.Role.BUSINESS),
                address(seller, AddressEntity.Role.REGISTERED));
    }

    private ReceiptDraft.Address address(SellerEntity seller, AddressEntity.Role role) {
        for (AddressEntity address : seller.addresses) {
            if (address.role == role) {
                return new ReceiptDraft.Address(address.street, address.postalCode, address.city, address.countryCode);
            }
        }
        return null;
    }

    private ReceiptDraft.ReceiptInfo header(ReceiptHeaderEntity header) {
        return new ReceiptDraft.ReceiptInfo(
                header.receiptNumber,
                header.issuedAt.atOffset(ZoneOffset.UTC),
                header.currency,
                header.orderNumber);
    }

    private List<ReceiptDraft.Item> items(List<ReceiptItemEntity> items) {
        List<ReceiptDraft.Item> result = new ArrayList<>();
        for (ReceiptItemEntity item : items) {
            result.add(new ReceiptDraft.Item(
                    item.description,
                    item.itemType,
                    item.quantity,
                    item.unit,
                    money(item.unitPrice),
                    money(item.lineTotal),
                    item.taxCategory));
        }
        return result;
    }

    private List<ReceiptDraft.TaxLine> taxLines(List<TaxSummaryEntity> lines) {
        List<ReceiptDraft.TaxLine> result = new ArrayList<>();
        for (TaxSummaryEntity line : lines) {
            result.add(new ReceiptDraft.TaxLine(line.taxCategory, line.taxRate, money(line.taxableSales), money(line.taxAmount)));
        }
        return result;
    }

    private ReceiptDraft.Totals totals(ReceiptTotalsEntity totals) {
        return new ReceiptDraft.Totals(money(totals.taxAmount), money(totals.grossAmount), money(totals.amountDue));
    }

    private List<ReceiptDraft.Payment> payments(List<PaymentEntity> payments) {
        List<ReceiptDraft.Payment> result = new ArrayList<>();
        for (PaymentEntity payment : payments) {
            result.add(new ReceiptDraft.Payment(payment.method, money(payment.amount), payment.transactionId));
        }
        return result;
    }

    private ReceiptDraft.FiscalData fiscal(FiscalDataEntity fiscal) {
        if (fiscal == null) {
            return null;
        }
        return new ReceiptDraft.FiscalData(
                fiscal.cashRegisterCode,
                fiscal.cashierCode,
                fiscal.fiscalDeviceNumber,
                fiscal.verificationHash,
                fiscal.rawIdentificationLine);
    }

    private List<String> unparsed(List<UnparsedLineEntity> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return lines.stream().map(line -> line.lineText).toList();
    }

    private String joinedFileNames(List<SubmittedImage> images) {
        String joined = images.stream()
                .map(SubmittedImage::fileName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
