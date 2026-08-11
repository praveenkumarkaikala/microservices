package com.fundmatrix.foliokyc.domain;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fundmatrix.foliokyc.common.BaseEntity;
import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import com.fundmatrix.foliokyc.domain.enums.ModeOfHolding;
import com.fundmatrix.foliokyc.domain.enums.TaxStatus;
import com.fundmatrix.foliokyc.dto.NomineeDetails;

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

  
    @Column(name = "folio_number", unique = true, length = 30)
    private String folioNumber;

   
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    
    @Column(name = "distributor_id")
    private Long distributorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false, length = 20)
    private TaxStatus taxStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_holding", nullable = false, length = 30)
    private ModeOfHolding modeOfHolding;

    @JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "json",name = "nomineeDetails")
	List<NomineeDetails> nomineeDetails;

    @Column(name = "bank_account_ref", length = 60)
    private String bankAccountRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FolioStatus status;
}
