package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.NotificationClient;
import com.fundmatrix.foliokyc.client.NotificationClient.NotificationRequest;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.domain.enums.Role;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import com.fundmatrix.foliokyc.dto.KycStatusDto;
import com.fundmatrix.foliokyc.dto.SubmitKycRequest;
import com.fundmatrix.foliokyc.repository.KycRecordRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** KYC record capture and verification workflow. */
@Service
public class KycService {

    private final KycRecordRepository kycRepository;
    private final NotificationClient notificationClient;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public KycService(KycRecordRepository kycRepository, NotificationClient notificationClient,
                      AuditService auditService, CurrentUserService currentUser, Mapper mapper) {
        this.kycRepository = kycRepository;
        this.notificationClient = notificationClient;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    /**
     * Investor self-submission: the investor uploads their own KYC details for verification.
     * The investor is the authenticated user - no one can submit KYC on another's behalf.
     * The record is created in PENDING state and awaits Fund Ops / Compliance verification.
     *
     * <p>This no longer round-trips through a local User table (User is owned by
     * auth-user-service) - the investor id and role come straight off the JWT claims via
     * CurrentUserService.
     */
    @Transactional
    public KycRecordDto submit(SubmitKycRequest req) {
        if (currentUser.getRole() != Role.INVESTOR) {
            throw new BusinessException("Only investors can submit KYC");
        }
        Long investorId = currentUser.getId();
        KycRecord record = KycRecord.builder()
                .investorId(investorId)
                .kycType(req.kycType())
                .documentType(req.documentType())
                .documentRef(req.documentRef())
                .kycStatus(KycStatus.PENDING)
                .build();
        record = kycRepository.save(record);
        auditService.record("KYC_SUBMIT", "KycRecord", record.getId(),
                "KYC submitted by investor " + investorId);
        notificationClient.notify(new NotificationRequest(investorId, "KYC",
                "Your KYC has been submitted and is pending verification"));
        return mapper.toKycDto(record);
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> list(KycStatus status) {
        List<KycRecord> records = (status == null)
                ? kycRepository.findAll() : kycRepository.findByKycStatus(status);
        return records.stream().map(mapper::toKycDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> listForInvestor(Long investorId) {
        return kycRepository.findByInvestorId(investorId).stream().map(mapper::toKycDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> mine() {
        return listForInvestor(currentUser.getId());
    }

    @Transactional
    public KycRecordDto updateStatus(Long id, KycStatus status) {
        KycRecord record = kycRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRecord", id));
        record.setKycStatus(status);
        if (status == KycStatus.COMPLIANT) {
            record.setVerifiedDate(LocalDate.now());
        }
        kycRepository.save(record);
        auditService.record("KYC_STATUS", "KycRecord", id, "KYC status set to " + status);
        notificationClient.notify(new NotificationRequest(record.getInvestorId(), "KYC",
                "Your KYC status is now " + status));
        return mapper.toKycDto(record);
    }

    /**
     * Internal cross-service lookup consumed by transaction-service (pre-transaction KYC
     * gate), nav-accounting-service, distributor-commission-service, compliance-service and
     * dashboard-service - mirrors the monolith's
     * KycRecordRepository.existsByInvestor_IdAndKycStatus(investorId, COMPLIANT) check that
     * used to run in-process.
     */
    @Transactional(readOnly = true)
    public KycStatusDto kycStatusFor(Long investorId) {
        boolean compliant = kycRepository.existsByInvestorIdAndKycStatus(investorId, KycStatus.COMPLIANT);
        String status = kycRepository.findByInvestorId(investorId).stream()
                .max(Comparator.comparing(KycRecord::getId))
                .map(r -> r.getKycStatus().name())
                .orElse(KycStatus.PENDING.name());
        if (compliant) {
            status = KycStatus.COMPLIANT.name();
        }
        return new KycStatusDto(investorId, status, compliant);
    }
}
