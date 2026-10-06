package pl.tomaszko.cheapskountant.receipt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "address")
public class AddressEntity extends Timestamped {

    public enum Role {
        BUSINESS,
        REGISTERED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    SellerEntity seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    Role role;

    @Column(name = "street", nullable = false, length = 512)
    String street;

    @Column(name = "postal_code", nullable = false, length = 16)
    String postalCode;

    @Column(name = "city", nullable = false, length = 256)
    String city;

    @Column(name = "country_code", nullable = false, length = 2)
    String countryCode;
}
