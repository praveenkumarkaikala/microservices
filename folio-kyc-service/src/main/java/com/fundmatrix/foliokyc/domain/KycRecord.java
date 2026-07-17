package com.fundmatrix.foliokyc.domain;

import com.fundmatrix.foliokyc.common.BaseEntity;
import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.domain.enums.KycType;
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

import java.time.LocalDate;

/**
 * KYC verification record for an investor. Tracks the KYC mode, the supporting document,
 * verification date and compliance status.
 *
 * <p>The investor is referenced by plain {@code investorId} rather than a JPA relation:
 * {@code User} is owned by auth-user-service, which this service does not have a local
 * table for. This is an independent plain-id column from InvestorFolio.investorId - no
 * JPA relation between KycRecord and InvestorFolio is needed even though they now live in
 * the same schema.
 */
@Entity
@Table(name = "kyc_records", indexes = {
        @Index(name = "idx_kyc_investor", columnList = "investor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false, length = 20)
    private KycType kycType;

    @Column(name = "document_type", length = 60)
    private String documentType;

    @Column(name = "document_ref", length = 60)
    private String documentRef;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;
}
