package com.fundmatrix.foliokyc.controller;

import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.dto.CreditUnitsRequest;
import com.fundmatrix.foliokyc.dto.DebitUnitsRequest;
import com.fundmatrix.foliokyc.dto.HoldingDto;
import com.fundmatrix.foliokyc.dto.HoldingPortifolio;
import com.fundmatrix.foliokyc.dto.RevalueRequest;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import com.fundmatrix.foliokyc.service.HoldingService;
import com.fundmatrix.foliokyc.service.Mapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/holdings")
@Tag(name = "Holdings (internal)", description = "Service-to-service holding queries and mutations")
public class HoldingInternalController {

    private final FolioHoldingRepository holdingRepository;
    private final InvestorFolioRepository folioRepository;
    private final HoldingService holdingService;
    private final Mapper mapper;

    public HoldingInternalController(FolioHoldingRepository holdingRepository,
                                     InvestorFolioRepository folioRepository,
                                     HoldingService holdingService, Mapper mapper) {
        this.holdingRepository = holdingRepository;
        this.folioRepository = folioRepository;
        this.holdingService = holdingService;
        this.mapper = mapper;
    }

   
    @GetMapping("/option/{optionId}")
    @Transactional(readOnly = true)
    public List<HoldingDto> byOption(@PathVariable Long optionId) {
        return holdingRepository.findByOptionId(optionId).stream()
                .map(h -> mapper.toInternalHoldingDto(h, h.getFolio().getInvestorId()))
                .toList();
    }

    @PostMapping("/option/{optionId}/revalue")
    public Integer revalue(@PathVariable Long optionId, @RequestBody RevalueRequest request) {
        return holdingService.revalueOption(optionId, request.navValue());
    }

    @PostMapping("/credit")
    @Transactional
    public HoldingDto credit(@RequestBody CreditUnitsRequest request) {
        FolioHolding holding = holdingService.creditUnits(request.folioId(), request.schemeId(),
                request.optionId(), request.units(), request.investedAmount(), request.navValue());
        return mapper.toInternalHoldingDto(holding, holding.getFolio().getInvestorId());
    }

    @PostMapping("/debit")
    @Transactional
    public HoldingDto debit(@RequestBody DebitUnitsRequest request) {
        FolioHolding holding = holdingService.debitUnits(request.folioId(), request.optionId(),
                request.units(), request.navValue());
        return mapper.toInternalHoldingDto(holding, holding.getFolio().getInvestorId());
    }

    @GetMapping("/aum/distributor/{distributorId}")
    public BigDecimal aum(@PathVariable Long distributorId,
                          @RequestParam(required = false) Long schemeId) {
        return schemeId != null
                ? holdingRepository.sumCurrentValueByDistributorAndScheme(distributorId, schemeId)
                : holdingRepository.sumCurrentValueByDistributor(distributorId);
    }

    @GetMapping("/folio-count/distributor/{distributorId}")
    public Long folioCount(@PathVariable Long distributorId) {
        return folioRepository.countByDistributorId(distributorId);
    }
    
    
    @GetMapping("/portfolio")
    public HoldingPortifolio getportfolio() {
        return holdingService.getinvestorPortfolio();
    }
    
    
}
