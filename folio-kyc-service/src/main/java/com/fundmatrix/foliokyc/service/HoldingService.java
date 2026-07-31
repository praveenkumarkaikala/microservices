package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.common.Calc;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.dto.FolioHoldingDto;
import com.fundmatrix.foliokyc.dto.HoldingPortifolio;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class HoldingService {

    private final FolioHoldingRepository holdingRepository;
    private final InvestorFolioRepository folioRepository;
    private final Mapper mapper;
    private final CurrentUserService currentUser;
    public HoldingService(FolioHoldingRepository holdingRepository,
                          InvestorFolioRepository folioRepository,Mapper mapper,CurrentUserService currentUser) {
        this.holdingRepository = holdingRepository;
        this.folioRepository = folioRepository;
        this.mapper=mapper;
        this.currentUser=currentUser;
    }

  
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
    
    
    
    @Transactional(readOnly = true)
    public HoldingPortifolio getinvestorPortfolio() {
    	List<InvestorFolio> folios=folioRepository.findByInvestorId(currentUser.getId());
		
		List<Long> folioIds=folios.stream().map((folio)->folio.getId()).toList();
		
		List<FolioHolding> holdings=holdingRepository.findByFolio_IdIn(folioIds);
		
		BigDecimal totalValue=holdings.stream().map((holding)->holding.getCurrentValue()).reduce(BigDecimal.ZERO,(sum,value)->sum.add(value));
		BigDecimal totalGainOrLoss=holdings.stream().map((holding)->holding.getUnrealisedGainLoss()).reduce(BigDecimal.ZERO,(sum,value)->sum.add(value));
		
//		List<FolioHoldingDto> holdingSummaries=holdings.stream().map((holding)->
//		{
//			BigDecimal latestNav=latestNavOrNull(holding.getOptionId());
//			return mapper.toHoldingDto(holding, latestNav);
//		}
//				).toList();
		return new HoldingPortifolio(currentUser.getId(), totalValue, totalGainOrLoss);
    }
}
