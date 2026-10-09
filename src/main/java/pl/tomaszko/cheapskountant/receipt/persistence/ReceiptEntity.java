package pl.tomaszko.cheapskountant.receipt.persistence;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt")
public class ReceiptEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "document_type", nullable = false, length = 32)
    String documentType;

    @Column(name = "source_file_name", length = 2048)
    String sourceFileName;

    @Column(name = "source_raw_text", length = 65535)
    String sourceRawText;

    @OneToOne(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    SellerEntity seller;

    @OneToOne(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    ReceiptHeaderEntity header;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    List<ReceiptItemEntity> items = new ArrayList<>();

    @OneToOne(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    DiscountSummaryEntity discountSummary;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    List<TaxSummaryEntity> taxSummaries = new ArrayList<>();

    @OneToOne(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    ReceiptTotalsEntity totals;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    List<PaymentEntity> payments = new ArrayList<>();

    @OneToOne(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    FiscalDataEntity fiscalData;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    List<UnparsedLineEntity> unparsedLines = new ArrayList<>();
}
