package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.domain.Allotment;
import com.fundmatrix.transaction.domain.SipMandate;
import com.fundmatrix.transaction.domain.SwpMandate;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.TransactionFlag;
import com.fundmatrix.transaction.dto.AllotmentDto;
import com.fundmatrix.transaction.dto.SipMandateDto;
import com.fundmatrix.transaction.dto.SwpMandateDto;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.dto.TransactionFlagDto;
import org.springframework.stereotype.Component;

/**
 * Maps entities to their outward DTOs. Folio identity is now a plain denormalized
 * folioId/folioNumber pair on each entity (no JPA relation to navigate - see the class
 * javadoc on Transaction/SipMandate/SwpMandate), and scheme/option names are no longer
 * navigable via JPA relations either (fund-catalog-service owns those) - callers fetch scheme
 * names via FundCatalogClient and pass them in, mirroring the existing pattern used for NAV
 * values.
 */
@Component
public class Mapper {

    public TransactionDto toTxnDto(Transaction t, String schemeName, String optionType) {
        return new TransactionDto(t.getId(), t.getTransactionRef(), t.getFolioId(),
                t.getFolioNumber(), t.getSchemeId(), schemeName,
                t.getOptionId(), optionType, t.getTransactionType(),
                t.getAmount(), t.getUnits(), t.getApplicableNav(), t.getTransactionDate(),
                t.getCutOffStatus(), t.getStatus(), t.getExitLoadAmount(), t.getRemarks());
    }

    public AllotmentDto toAllotmentDto(Allotment a) {
        return new AllotmentDto(a.getId(), a.getTransaction().getId(),
                a.getTransaction().getTransactionRef(), a.getUnitsAllotted(), a.getAllotmentNav(),
                a.getAllotmentDate(), a.getStatus());
    }

    public SipMandateDto toSipDto(SipMandate s, String schemeName) {
        return new SipMandateDto(s.getId(), s.getMandateRef(), s.getFolioId(),
                s.getFolioNumber(), s.getSchemeId(), schemeName,
                s.getOptionId(), s.getAmount(), s.getFrequency(), s.getStartDate(), s.getEndDate(),
                s.getInstalmentCount(), s.getInstalmentsExecuted(), s.getNextInstalmentDate(), s.getStatus());
    }

    public SwpMandateDto toSwpDto(SwpMandate s, String schemeName) {
        return new SwpMandateDto(s.getId(), s.getMandateRef(), s.getFolioId(),
                s.getFolioNumber(), s.getSchemeId(), schemeName,
                s.getOptionId(), s.getAmount(), s.getFrequency(), s.getStartDate(), s.getEndDate(),
                s.getInstalmentCount(), s.getInstalmentsExecuted(), s.getNextInstalmentDate(), s.getStatus());
    }

    public TransactionFlagDto toFlagDto(TransactionFlag f, String schemeName) {
        Transaction t = f.getTransaction();
        return new TransactionFlagDto(f.getId(), t.getId(), t.getTransactionRef(),
                t.getFolioNumber(), schemeName, f.getAmount(),
                f.getReason(), f.getStatus(), f.getReviewNote(), f.getCreatedDate(), f.getReviewedDate());
    }
}
