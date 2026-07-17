package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private FolioHoldingRepository holdingRepository;
    @Mock
    private InvestorFolioRepository folioRepository;

    @InjectMocks
    private HoldingService holdingService;

    private InvestorFolio folio;

    @BeforeEach
    void setUp() {
        folio = InvestorFolio.builder().build();
        folio.setId(1L);
        folio.setFolioNumber("FOL00001");
        folio.setInvestorId(10L);
    }

    @Test
    void creditUnits_newHolding_setsWeightedAverageCostAndRevalues() {
        when(folioRepository.findById(1L)).thenReturn(Optional.of(folio));
        when(holdingRepository.findByFolio_IdAndOptionId(1L, 5L)).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FolioHolding result = holdingService.creditUnits(1L, 2L, 5L,
                new BigDecimal("100.0000"), new BigDecimal("1000.00"), new BigDecimal("10.0000"));

        assertThat(result.getUnitsHeld()).isEqualByComparingTo("100.0000");
        assertThat(result.getAverageCostNav()).isEqualByComparingTo("10.0000");
        assertThat(result.getCurrentValue()).isEqualByComparingTo("1000.00");
        assertThat(result.getUnrealisedGainLoss()).isEqualByComparingTo("0.00");
    }

    @Test
    void creditUnits_existingHolding_blendsAverageCost() {
        FolioHolding existing = FolioHolding.builder()
                .folio(folio).schemeId(2L).optionId(5L)
                .unitsHeld(new BigDecimal("100.0000"))
                .averageCostNav(new BigDecimal("10.0000"))
                .build();
        when(folioRepository.findById(1L)).thenReturn(Optional.of(folio));
        when(holdingRepository.findByFolio_IdAndOptionId(1L, 5L)).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // add 100 units for 2000 invested (NAV 20) -> blended cost = (1000+2000)/200 = 15
        FolioHolding result = holdingService.creditUnits(1L, 2L, 5L,
                new BigDecimal("100.0000"), new BigDecimal("2000.00"), new BigDecimal("20.0000"));

        assertThat(result.getUnitsHeld()).isEqualByComparingTo("200.0000");
        assertThat(result.getAverageCostNav()).isEqualByComparingTo("15.0000");
        assertThat(result.getCurrentValue()).isEqualByComparingTo("4000.00");
        // cost basis = 200 * 15 = 3000; value 4000 -> gain 1000
        assertThat(result.getUnrealisedGainLoss()).isEqualByComparingTo("1000.00");
    }

    @Test
    void debitUnits_insufficientUnits_throws() {
        FolioHolding existing = FolioHolding.builder()
                .folio(folio).schemeId(2L).optionId(5L)
                .unitsHeld(new BigDecimal("10.0000"))
                .averageCostNav(new BigDecimal("10.0000"))
                .build();
        when(holdingRepository.findByFolio_IdAndOptionId(1L, 5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> holdingService.debitUnits(folio.getId(), 5L, new BigDecimal("50.0000"),
                new BigDecimal("10.0000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient units");
    }

    @Test
    void debitUnits_reducesUnitsAndRevalues() {
        FolioHolding existing = FolioHolding.builder()
                .folio(folio).schemeId(2L).optionId(5L)
                .unitsHeld(new BigDecimal("100.0000"))
                .averageCostNav(new BigDecimal("10.0000"))
                .build();
        when(holdingRepository.findByFolio_IdAndOptionId(1L, 5L)).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FolioHolding result = holdingService.debitUnits(folio.getId(), 5L, new BigDecimal("40.0000"),
                new BigDecimal("12.0000"));

        assertThat(result.getUnitsHeld()).isEqualByComparingTo("60.0000");
        assertThat(result.getCurrentValue()).isEqualByComparingTo("720.00");
    }

    @Test
    void revalueOption_updatesAllHoldingsInOption() {
        FolioHolding h1 = FolioHolding.builder().folio(folio).schemeId(2L).optionId(5L)
                .unitsHeld(new BigDecimal("10.0000")).averageCostNav(BigDecimal.TEN).build();
        FolioHolding h2 = FolioHolding.builder().folio(folio).schemeId(2L).optionId(5L)
                .unitsHeld(new BigDecimal("20.0000")).averageCostNav(BigDecimal.TEN).build();
        when(holdingRepository.findByOptionId(5L)).thenReturn(List.of(h1, h2));
        when(holdingRepository.saveAll(any())).thenReturn(List.of(h1, h2));

        int count = holdingService.revalueOption(5L, new BigDecimal("11.0000"));

        assertThat(count).isEqualTo(2);
        assertThat(h1.getCurrentValue()).isEqualByComparingTo("110.00");
        assertThat(h2.getCurrentValue()).isEqualByComparingTo("220.00");
    }
}
