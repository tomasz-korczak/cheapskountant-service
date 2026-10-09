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
@Table(name = "receipt_item")
public class ReceiptItemEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    ReceiptEntity receipt;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @Column(name = "description", nullable = false, length = 1024)
    String description;

    @Column(name = "item_type", length = 32)
    String itemType;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    BigDecimal quantity;

    @Column(name = "unit", length = 32)
    String unit;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    BigDecimal lineTotal;

    @Column(name = "tax_category", nullable = false, length = 32)
    String taxCategory;

    @Column(name = "discount_description", length = 1024)
    String discountDescription;

    @Column(name = "discount_total", precision = 14, scale = 2)
    BigDecimal discountTotal;
}
