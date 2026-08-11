package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.dto.FolioDto;
import com.fundmatrix.foliokyc.dto.FolioHoldingDto;
import com.fundmatrix.foliokyc.dto.HoldingDto;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Mapper {

    public FolioDto toFolioDto(InvestorFolio f, BigDecimal currentValue, String investorName,String distributorName) {
        return new FolioDto(f.getId(), f.getFolioNumber(), f.getInvestorId(), investorName,
                f.getDistributorId(), f.getTaxStatus(), f.getModeOfHolding(), f.getNomineeDetails(),
                f.getBankAccountRef(), f.getStatus(), currentValue,distributorName);
    }

    public FolioHoldingDto toHoldingDto(FolioHolding h, BigDecimal latestNav, String schemeName,
                                        String optionType) {
        return new FolioHoldingDto(h.getId(), h.getFolio().getId(), h.getFolio().getFolioNumber(),
                h.getSchemeId(), schemeName, h.getOptionId(), optionType, h.getUnitsHeld(),
                h.getAverageCostNav(), latestNav, h.getCurrentValue(), h.getUnrealisedGainLoss(),
                h.getLastUpdated());
    }

    public HoldingDto toInternalHoldingDto(FolioHolding h, Long investorId) {
        return new HoldingDto(h.getId(), h.getFolio().getId(), h.getSchemeId(), h.getOptionId(),
                h.getUnitsHeld(), h.getAverageCostNav(), h.getCurrentValue(), h.getUnrealisedGainLoss(),
                investorId);
    }

    public KycRecordDto toKycDto(KycRecord k) {
        return new KycRecordDto(k.getId(), k.getInvestorId(),null, k.getKycType(), k.getDocumentType(),
                k.getDocumentRef(), k.getVerifiedDate(), k.getKycStatus());
    }
    
    public KycRecordDto toKycDto(KycRecord k,String investorName) {
        return new KycRecordDto(k.getId(), k.getInvestorId(),investorName, k.getKycType(), k.getDocumentType(),
                k.getDocumentRef(), k.getVerifiedDate(), k.getKycStatus());
    }
}
