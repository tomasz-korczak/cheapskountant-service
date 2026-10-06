package pl.tomaszko.cheapskountant.receipt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "unparsed_line")
public class UnparsedLineEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    ReceiptEntity receipt;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @Column(name = "line_text", nullable = false, length = 65535)
    String lineText;
}
