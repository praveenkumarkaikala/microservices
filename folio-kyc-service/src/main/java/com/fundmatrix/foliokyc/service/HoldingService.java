package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.common.Calc;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Central authority for unit-holding mathematics: crediting/debiting units, weighted
 * average cost maintenance, and NAV-based revaluation. Keeps allotment, dividend
 * reinvestment and NAV publication consistent.
 *
 * NOTE: NAV lookups are performed by transaction-service (which owns NavAccountingClient)
 * and the resolved navValue is passed into creditUnits/debitUnits/revalueOption below via
 * the internal /holdings/** endpoints, exactly as they already did for the underlying
 * arithmetic.
 */
@Service
public class HoldingService {

    private final FolioHoldingRepository holdingRepository;
    private final InvestorFolioRepository folioRepository;

    public HoldingService(FolioHoldingRepository holdingRepository,
                          InvestorFolioRepository folioRepository) {
        this.holdingRepository = holdingRepository;
        this.folioRepository = folioRepository;
    }

    /**
     * Adds units to a holding (creating it if necessary) and updates the weighted-average
     * cost using the invested amount, then revalues at the given NAV. Callers now only
     * hold ids (folio/scheme/option), since only folio is a local aggregate and even it
     * may be known only by id (e.g. the internal /holdings/credit endpoint) - the folio is
     * always (re-)loaded here to keep the signature uniform for both in-process and
     * Feign-driven callers.
     */
    @Transactional
    public FolioHolding creditUnits(Long folioId, Long schemeId, Long optionId,
                                    BigDecimal addUnits, BigDecimal investedAmount, BigDecimal navValue) {
        InvestorFolio folio = folioRepository.findById(folioId)
                .orElseThrow(() -> ResourceNotFoundException.of("InvestorFolio", folioId));

        FolioHolding holding = holdingRepository
                .findByFolio_IdAndOptionId(folio.getId(), optionId)
                .orElseGet(() -> FolioHolding.builder()
                        .folio(folio).schemeId(schemeId).optionId(optionId)
                        .unitsHeld(BigDecimal.ZERO).averageCostNav(BigDecimal.ZERO)
                        .build());

        BigDecimal oldUnits = Calc.nz(holding.getUnitsHeld());
        BigDecimal oldCost = oldUnits.multiply(Calc.nz(holding.getAverageCostNav()));
        BigDecimal newUnits = oldUnits.add(addUnits);
        BigDecimal newCost = oldCost.add(Calc.nz(investedAmount));

        holding.setUnitsHeld(Calc.units(newUnits));
        holding.setAverageCostNav(newUnits.signum() > 0
                ? newCost.divide(newUnits, Calc.UNIT_SCALE, Calc.RM)
                : BigDecimal.ZERO);
        revalue(holding, navValue);
        return holdingRepository.save(holding);
    }

    /**
     * Removes units from a holding; average cost is unchanged on redemption. Takes folioId
     * (not an InvestorFolio reference) so it works identically for the internal
     * /holdings/debit endpoint as for any in-process caller.
     */
    @Transactional
    public FolioHolding debitUnits(Long folioId, Long optionId,
                                   BigDecimal redeemUnits, BigDecimal navValue) {
        FolioHolding holding = holdingRepository
                .findByFolio_IdAndOptionId(folioId, optionId)
                .orElseThrow(() -> new BusinessException("No holding to redeem for this option"));

        BigDecimal available = Calc.nz(holding.getUnitsHeld());
        if (redeemUnits.compareTo(available) > 0) {
            throw new BusinessException("Insufficient units: holding " + available
                    + ", requested " + redeemUnits);
        }
        holding.setUnitsHeld(Calc.units(available.subtract(redeemUnits)));
        revalue(holding, navValue);
        return holdingRepository.save(holding);
    }

    /** Revalues every holding in an option to a new NAV — used on NAV publication. */
    @Transactional
    public int revalueOption(Long optionId, BigDecimal navValue) {
        List<FolioHolding> holdings = holdingRepository.findByOptionId(optionId);
        for (FolioHolding h : holdings) {
            revalue(h, navValue);
        }
        holdingRepository.saveAll(holdings);
        return holdings.size();
    }

    private void revalue(FolioHolding holding, BigDecimal navValue) {
        BigDecimal units = Calc.nz(holding.getUnitsHeld());
        if (navValue != null) {
            BigDecimal value = Calc.amountFor(units, navValue);
            holding.setCurrentValue(value);
            BigDecimal cost = units.multiply(Calc.nz(holding.getAverageCostNav()));
            holding.setUnrealisedGainLoss(Calc.money(value.subtract(cost)));
        }
        holding.setLastUpdated(Instant.now());
    }
}
