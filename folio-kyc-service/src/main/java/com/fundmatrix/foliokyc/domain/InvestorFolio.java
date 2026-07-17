package com.fundmatrix.foliokyc.domain;

import com.fundmatrix.foliokyc.common.BaseEntity;
import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import com.fundmatrix.foliokyc.domain.enums.ModeOfHolding;
import com.fundmatrix.foliokyc.domain.enums.TaxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An investor's account (folio) within the AMC. The owning investor (auth-user-service)
 * and optional servicing distributor (distributor-commission-service) now live in other
 * microservices, so they are referenced here only by plain id columns - no JPA relation.
 */
@Entity
@Table(name = "investor_folios", indexes = {
        @Index(name = "idx_folio_number", columnList = "folio_number", unique = true),
        @Index(name = "idx_folio_investor", columnList = "investor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorFolio extends BaseEntity {

    /** Human-readable folio number, e.g. "FOL00001"; assigned from the id immediately after insert. */
    @Column(name = "folio_number", unique = true, length = 30)
    private String folioNumber;

    /** Owning investor's user id (auth-user-service). */
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** Servicing distributor's id (distributor-commission-service), if any. */
    @Column(name = "distributor_id")
    private Long distributorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false, length = 20)
    private TaxStatus taxStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_holding", nullable = false, length = 30)
    private ModeOfHolding modeOfHolding;

    @Column(name = "nominee_details", length = 255)
    private String nomineeDetails;

    @Column(name = "bank_account_ref", length = 60)
    private String bankAccountRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FolioStatus status;
}
