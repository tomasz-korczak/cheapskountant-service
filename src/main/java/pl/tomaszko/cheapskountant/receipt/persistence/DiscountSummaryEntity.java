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
@Table(name = "discount_summary")
public class DiscountSummaryEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    ReceiptEntity receipt;

    @Column(name = "description", nullable = false, length = 1024)
    String description;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    BigDecimal total;
}
