package com.fundmatrix.fundcatalog.domain;

import com.fundmatrix.fundcatalog.common.BaseEntity;
import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.OptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A plan/option under a {@link FundScheme} (Growth, Dividend Payout, Dividend Reinvestment),
 * uniquely identified by its ISIN.
 */
@Entity
@Table(name = "scheme_options", indexes = {
        @Index(name = "idx_option_isin", columnList = "isin", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 30)
    private OptionType optionType;

    @Column(unique = true, length = 20)
    private String isin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptionStatus status;
}
