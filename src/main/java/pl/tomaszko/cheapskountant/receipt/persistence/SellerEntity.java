package pl.tomaszko.cheapskountant.receipt.persistence;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller")
public class SellerEntity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    ReceiptEntity receipt;

    @Column(name = "trade_name", nullable = false, length = 512)
    String tradeName;

    @Column(name = "legal_name", length = 512)
    String legalName;

    @Column(name = "tax_id", nullable = false, length = 10)
    String taxId;

    @Column(name = "bdo_number", length = 9)
    String bdoNumber;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    List<AddressEntity> addresses = new ArrayList<>();
}
