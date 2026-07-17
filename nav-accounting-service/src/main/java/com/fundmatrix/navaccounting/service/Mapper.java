package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.domain.DividendDeclaration;
import com.fundmatrix.navaccounting.domain.FundExpenseAccrual;
import com.fundmatrix.navaccounting.domain.InvestorDividendEntitlement;
import com.fundmatrix.navaccounting.domain.NavRecord;
import com.fundmatrix.navaccounting.dto.DividendDeclarationDto;
import com.fundmatrix.navaccounting.dto.EntitlementDto;
import com.fundmatrix.navaccounting.dto.ExpenseAccrualDto;
import com.fundmatrix.navaccounting.dto.NavRecordDto;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

    public NavRecordDto toNavDto(NavRecord n) {
        return new NavRecordDto(n.getId(), n.getSchemeId(), n.getSchemeName(),
                n.getOptionId(), n.getOptionType(), n.getNavDate(),
                n.getNavValue(), n.getTotalAum(), n.getTotalUnitsOutstanding(), n.getPublishedById(),
                n.getStatus());
    }

    public ExpenseAccrualDto toAccrualDto(FundExpenseAccrual a) {
        return new ExpenseAccrualDto(a.getId(), a.getSchemeId(), a.getSchemeName(),
                a.getExpenseType(), a.getAccrualAmount(), a.getAccrualDate(), a.getAnnualisedRate(),
                a.getStatus(), a.getReversalReason());
    }

    public DividendDeclarationDto toDividendDto(DividendDeclaration d, long entitlementCount) {
        return new DividendDeclarationDto(d.getId(), d.getSchemeId(), d.getSchemeName(),
                d.getOptionId(), d.getOptionType() != null ? d.getOptionType().name() : null, d.getRecordDate(),
                d.getDividendPerUnit(), d.getTotalDistributionAmount(), d.getDeclaredById(), d.getStatus(),
                entitlementCount);
    }

    public EntitlementDto toEntitlementDto(InvestorDividendEntitlement e) {
        return new EntitlementDto(e.getId(), e.getDeclaration().getId(), e.getFolioId(), e.getInvestorId(),
                e.getUnitsOnRecordDate(), e.getGrossDividend(), e.getTaxDeducted(), e.getNetDividend(),
                e.getPayoutMode(), e.getStatus());
    }
}
