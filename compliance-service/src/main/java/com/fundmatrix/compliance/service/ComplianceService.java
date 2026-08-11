package com.fundmatrix.compliance.service;

import com.fundmatrix.compliance.client.FolioKycClient;
import com.fundmatrix.compliance.client.TransactionClient;
import com.fundmatrix.compliance.common.exception.BusinessException;
import com.fundmatrix.compliance.common.exception.ResourceNotFoundException;
import com.fundmatrix.compliance.domain.enums.FlagStatus;
import com.fundmatrix.compliance.dto.ComplianceKycStatusDto;
import com.fundmatrix.compliance.dto.ComplianceReportDto;
import com.fundmatrix.compliance.dto.KycRecordDto;
import com.fundmatrix.compliance.dto.TransactionFlagDto;
import com.fundmatrix.compliance.security.CurrentUserService;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ComplianceService {

    private static final String COMPLIANT = "COMPLIANT";
    private static final String PENDING = "PENDING";
    private static final String NON_COMPLIANT = "NON_COMPLIANT";
    private static final String EXPIRED = "EXPIRED";

    private final FolioKycClient folioKycClient;
    private final TransactionClient transactionClient;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public ComplianceService(FolioKycClient folioKycClient, TransactionClient transactionClient,
                             CurrentUserService currentUser, AuditService auditService) {
        this.folioKycClient = folioKycClient;
        this.transactionClient = transactionClient;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    
    public ComplianceKycStatusDto kycStatus() {
        List<KycRecordDto> all;
        try {
            all = folioKycClient.listKyc(null);
        } catch (FeignException ex) {
            throw new BusinessException("Unable to retrieve KYC records: " + ex.getMessage());
        }
        long compliant = countByStatus(all, COMPLIANT);
        long pending = countByStatus(all, PENDING);
        long nonCompliant = countByStatus(all, NON_COMPLIANT);
        long expired = countByStatus(all, EXPIRED);
        return new ComplianceKycStatusDto(compliant, pending, nonCompliant, expired, all.size());
    }

 
    public List<TransactionFlagDto> flags(FlagStatus status) {
        try {
            return transactionClient.flags(status == null ? null : status.name());
        } catch (FeignException ex) {
            throw new BusinessException("Unable to retrieve transaction flags: " + ex.getMessage());
        }
    }

   
    public TransactionFlagDto reviewFlag(Long id, FlagStatus to, String note) {
        Map<String, String> body = new HashMap<>();
        body.put("status", to.name());
        if (note != null) {
            body.put("note", note);
        }
        TransactionFlagDto updated;
        try {
            updated = transactionClient.reviewFlag(id, body);
        } catch (FeignException.NotFound ex) {
            throw ResourceNotFoundException.of("TransactionFlag", id);
        } catch (FeignException ex) {
            throw new BusinessException("Cannot move flag " + id + " to " + to + ": " + ex.getMessage());
        }
        auditService.record("FLAG_REVIEW", "TransactionFlag", id,
                "Flag -> " + to + (note != null && !note.isBlank() ? " (" + note + ")" : ""));
        return updated;
    }

    public ComplianceReportDto generateReport() {
        List<TransactionFlagDto> all = flags(null);
        BigDecimal total = all.stream().map(f -> f.amount() == null ? BigDecimal.ZERO : f.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ComplianceReportDto report = new ComplianceReportDto(
                Instant.now(),
                currentUser.getEmail(),
                kycStatus(),
                countByStatus(all, FlagStatus.OPEN),
                countByStatus(all, FlagStatus.REVIEWED),
                countByStatus(all, FlagStatus.CLEARED),
                countByStatus(all, FlagStatus.ESCALATED),
                all.size(),
                total.setScale(2, java.math.RoundingMode.HALF_UP));
        auditService.record("COMPLIANCE_REPORT", "Report", null, "Generated compliance report");
        return report;
    }

    private long countByStatus(List<TransactionFlagDto> flags, FlagStatus status) {
        return flags.stream().filter(f -> f.status() == status).count();
    }

    private long countByStatus(List<KycRecordDto> records, String kycStatus) {
        return records.stream().filter(r -> kycStatus.equals(r.kycStatus())).count();
    }
}
