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
@Table(name = "payment")
public class PaymentEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    ReceiptEntity receipt;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @Column(name = "method", nullable = false, length = 16)
    String method;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    BigDecimal amount;

    @Column(name = "transaction_id", length = 128)
    String transactionId;
}
