package pl.tomaszko.cheapskountant.receipt.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_totals")
public class ReceiptTotalsEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    ReceiptEntity receipt;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal taxAmount;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal grossAmount;

    @Column(name = "amount_due", nullable = false, precision = 14, scale = 2)
    BigDecimal amountDue;
}
