package app.adapters.persistence.jpa.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "buyers")
@Getter
@Setter
@NoArgsConstructor
public class BuyerJpaEntity {

    @Id
    @Column(name = "user_identifier", nullable = false, length = 36)
    private String userIdentifier;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_identifier", nullable = false)
    private UserJpaEntity user;

    @Column(name = "primary_address", nullable = false, length = 500)
    private String primaryAddress;

    @Column(name = "buyer_status_code", nullable = false, length = 40)
    private String buyerStatusCode;

    @ElementCollection
    @CollectionTable(name = "buyer_additional_addresses",
            joinColumns = @JoinColumn(name = "user_identifier"))
    @OrderColumn(name = "position")
    @Column(name = "address", nullable = false, length = 500)
    private List<String> additionalAddresses = new ArrayList<>();
}