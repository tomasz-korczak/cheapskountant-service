package pl.tomaszko.cheapskountant.receipt.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tax_summary")
public class TaxSummaryEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    ReceiptEntity receipt;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @Column(name = "tax_category", nullable = false, length = 32)
    String taxCategory;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    BigDecimal taxRate;

    @Column(name = "taxable_sales", nullable = false, precision = 14, scale = 2)
    BigDecimal taxableSales;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal taxAmount;
}
