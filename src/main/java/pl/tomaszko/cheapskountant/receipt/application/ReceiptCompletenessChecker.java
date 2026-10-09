package pl.tomaszko.cheapskountant.receipt.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import pl.tomaszko.cheapskountant.expense.HouseholdCategories;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.transcription.ReceiptDraft;

@Component
public class ReceiptCompletenessChecker {

    private static final Pattern MONEY = Pattern.compile("^-?[0-9]+\\.[0-9]{2}$");
    private static final Pattern TAX_ID = Pattern.compile("^[0-9]{10}$");
    private static final Pattern BDO = Pattern.compile("^[0-9]{9}$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern POSTAL_CODE = Pattern.compile("^[0-9]{2}-[0-9]{3}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");
    private static final Set<String> PAYMENT_METHODS = Set.of("cash", "card", "transfer", "voucher", "mobile", "other");
    private static final Set<String> ITEM_TYPES = Set.of("product", "service", "packaging", "other");

    public void check(ReceiptDraft draft) {
        if (draft == null || draft.documentType() == null || !"fiscal_receipt".equals(draft.documentType())) {
            throw ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt.");
        }
        checkSeller(draft.seller());
        checkHeader(draft.receipt());
        checkItems(draft.items());
        checkDiscount(draft.discountSummary(), "The discount summary is incomplete.");
        checkTaxSummary(draft.taxSummary());
        checkTotals(draft.totals());
        checkPayments(draft.payments());
    }

    private void checkSeller(ReceiptDraft.Seller seller) {
        if (seller == null || blank(seller.tradeName())) {
            throw ReceiptFailureException.incomplete("The seller trade name is missing.");
        }
        if (seller.taxId() == null || !TAX_ID.matcher(seller.taxId()).matches()) {
            throw ReceiptFailureException.incomplete("The seller tax identifier is missing.");
        }
        if (seller.bdoNumber() != null && !BDO.matcher(seller.bdoNumber()).matches()) {
            throw ReceiptFailureException.incomplete("The seller BDO number is invalid.");
        }
        checkAddress(seller.businessAddress());
        checkAddress(seller.registeredAddress());
    }

    private void checkAddress(ReceiptDraft.Address address) {
        if (address == null) {
            return;
        }
        if (blank(address.street()) || blank(address.postalCode()) || blank(address.city()) || blank(address.countryCode())) {
            throw ReceiptFailureException.incomplete("The seller address is incomplete.");
        }
        if (!POSTAL_CODE.matcher(address.postalCode()).matches() || !COUNTRY.matcher(address.countryCode()).matches()) {
            throw ReceiptFailureException.incomplete("The seller address is incomplete.");
        }
    }

    private void checkHeader(ReceiptDraft.ReceiptInfo receipt) {
        if (receipt == null || blank(receipt.number()) || receipt.issuedAt() == null) {
            throw ReceiptFailureException.incomplete("The receipt header is incomplete.");
        }
        if (receipt.currency() == null || !CURRENCY.matcher(receipt.currency()).matches()) {
            throw ReceiptFailureException.incomplete("The receipt currency is invalid.");
        }
    }

    private void checkItems(List<ReceiptDraft.Item> items) {
        if (items == null || items.isEmpty()) {
            throw ReceiptFailureException.incomplete("The receipt has no line items.");
        }
        for (ReceiptDraft.Item item : items) {
            if (item == null
                    || blank(item.description())
                    || item.quantity() == null
                    || item.quantity().compareTo(BigDecimal.ZERO) <= 0
                    || !money(item.unitPrice())
                    || !money(item.total())
                    || blank(item.taxCategory())
                    || !HouseholdCategories.receipt(item.category())) {
                throw ReceiptFailureException.incomplete("A line item is incomplete.");
            }
            if (item.itemType() != null && !ITEM_TYPES.contains(item.itemType())) {
                throw ReceiptFailureException.incomplete("A line item is incomplete.");
            }
            checkDiscount(item.discount(), "An item discount is incomplete.");
        }
    }

    private void checkDiscount(ReceiptDraft.Discount discount, String message) {
        if (discount == null) {
            return;
        }
        if (blank(discount.description()) || !negativeMoney(discount.total())) {
            throw ReceiptFailureException.incomplete(message);
        }
    }

    private void checkTaxSummary(List<ReceiptDraft.TaxLine> taxSummary) {
        if (taxSummary == null || taxSummary.isEmpty()) {
            throw ReceiptFailureException.incomplete("The tax summary is missing.");
        }
        for (ReceiptDraft.TaxLine line : taxSummary) {
            if (line == null
                    || blank(line.taxCategory())
                    || line.taxRate() == null
                    || line.taxRate().compareTo(BigDecimal.ZERO) < 0
                    || line.taxRate().compareTo(new BigDecimal("100")) > 0
                    || !money(line.taxableSales())
                    || !money(line.taxAmount())) {
                throw ReceiptFailureException.incomplete("A tax summary entry is incomplete.");
            }
        }
    }

    private void checkTotals(ReceiptDraft.Totals totals) {
        if (totals == null || !money(totals.taxAmount()) || !money(totals.grossAmount()) || !money(totals.amountDue())) {
            throw ReceiptFailureException.incomplete("The receipt totals are incomplete.");
        }
    }

    private void checkPayments(List<ReceiptDraft.Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            throw ReceiptFailureException.incomplete("The receipt has no payment.");
        }
        for (ReceiptDraft.Payment payment : payments) {
            if (payment == null || payment.method() == null || !PAYMENT_METHODS.contains(payment.method()) || !money(payment.amount())) {
                throw ReceiptFailureException.incomplete("A payment is incomplete.");
            }
        }
    }

    private boolean money(String value) {
        return value != null && MONEY.matcher(value).matches();
    }

    private boolean negativeMoney(String value) {
        return money(value) && new BigDecimal(value).compareTo(BigDecimal.ZERO) < 0;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
