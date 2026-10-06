package pl.tomaszko.cheapskountant.receipt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fiscal_data")
public class FiscalDataEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    ReceiptEntity receipt;

    @Column(name = "cash_register_code", length = 64)
    String cashRegisterCode;

    @Column(name = "cashier_code", length = 64)
    String cashierCode;

    @Column(name = "fiscal_device_number", length = 128)
    String fiscalDeviceNumber;

    @Column(name = "verification_hash", length = 256)
    String verificationHash;

    @Column(name = "raw_identification_line", length = 512)
    String rawIdentificationLine;
}
