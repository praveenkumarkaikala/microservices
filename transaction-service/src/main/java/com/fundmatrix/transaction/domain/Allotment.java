package com.fundmatrix.transaction.domain;

import com.fundmatrix.transaction.common.BaseEntity;
import com.fundmatrix.transaction.domain.enums.AllotmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The unit-allotment outcome for an accepted {@link Transaction}: the number of units
 * allotted at the applicable NAV on the allotment date.
 */
@Entity
@Table(name = "allotments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allotment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(name = "units_allotted", precision = 19, scale = 4)
    private BigDecimal unitsAllotted;

    @Column(name = "allotment_nav", precision = 19, scale = 4)
    private BigDecimal allotmentNav;

    @Column(name = "allotment_date")
    private LocalDate allotmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllotmentStatus status;
}
