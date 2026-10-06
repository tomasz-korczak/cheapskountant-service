package pl.tomaszko.cheapskountant.receipt.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_header")
public class ReceiptHeaderEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    ReceiptEntity receipt;

    @Column(name = "receipt_number", nullable = false, length = 128)
    String receiptNumber;

    @Column(name = "issued_at", nullable = false)
    LocalDateTime issuedAt;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "order_number", length = 128)
    String orderNumber;
}
