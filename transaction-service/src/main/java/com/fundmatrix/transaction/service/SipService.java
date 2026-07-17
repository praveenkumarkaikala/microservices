package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.common.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.domain.SipMandate;
import com.fundmatrix.transaction.domain.enums.NotificationCategory;
import com.fundmatrix.transaction.domain.enums.Role;
import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import com.fundmatrix.transaction.dto.CreateSipRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.SchemeDto;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SipMandateDto;
import com.fundmatrix.transaction.repository.SipMandateRepository;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Systematic Investment Plan mandate management and instalment execution.
 * Scheme/option data (min SIP amount, scheme name) comes from {@link FundCatalogClient}.
 * Folio access-control/status checks go through {@link FolioAccessService} (Feign to
 * folio-kyc-service) instead of an in-process FolioService - SipService/SwpService/
 * TransactionService still call each other in-process (all three remain local to this one
 * transaction-service module), only the folio/holdings/KYC half moved out.
 */
@Service
public class SipService {

    private final SipMandateRepository sipRepository;
    private final FundCatalogClient fundCatalogClient;
    private final FolioAccessService folioAccessService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public SipService(SipMandateRepository sipRepository, FundCatalogClient fundCatalogClient,
                      FolioAccessService folioAccessService, TransactionService transactionService,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.sipRepository = sipRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.folioAccessService = folioAccessService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public SipMandateDto create(CreateSipRequest req) {
        FolioDto folio = folioAccessService.loadAccessible(req.folioId());
        if (!"ACTIVE".equals(folio.status())) {
            throw new BusinessException("Folio is not active");
        }
        SchemeOptionDto option = fundCatalogClient.getOption(req.optionId());
        if (option == null) {
            throw ResourceNotFoundException.of("SchemeOption", req.optionId());
        }
        SchemeDto scheme = fundCatalogClient.getScheme(option.schemeId());

        var minSip = scheme != null ? scheme.minSipAmount() : null;
        if (minSip != null && req.amount().compareTo(minSip) < 0) {
            throw new BusinessException("Minimum SIP amount for " + option.schemeName()
                    + " is " + minSip);
        }

        SipMandate mandate = SipMandate.builder()
                .folioId(folio.id()).folioNumber(folio.folioNumber()).investorId(folio.investorId())
                .distributorId(folio.distributorId())
                .schemeId(option.schemeId()).optionId(option.id())
                .amount(req.amount())
                .frequency(req.frequency())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .instalmentCount(req.instalmentCount())
                .instalmentsExecuted(0)
                .nextInstalmentDate(req.startDate())
                .status(SipStatus.ACTIVE)
                .build();
        mandate = sipRepository.save(mandate);
        mandate.setMandateRef(String.format("SIP%06d", mandate.getId()));
        mandate = sipRepository.save(mandate);

        notificationService.notify(folio.investorId(), NotificationCategory.SIP,
                "SIP mandate " + mandate.getMandateRef() + " for " + req.amount() + " ("
                        + req.frequency() + ") registered");
        auditService.record("SIP_CREATE", "SipMandate", mandate.getId(),
                "SIP " + req.amount() + " " + req.frequency() + " folio " + folio.folioNumber());
        return mapper.toSipDto(mandate, option.schemeName());
    }

    @Transactional(readOnly = true)
    public List<SipMandateDto> listForCurrentUser() {
        List<SipMandate> mandates = (currentUser.getRole() == Role.INVESTOR)
                ? sipRepository.findByInvestorId(currentUser.getId())
                : sipRepository.findAll();
        return mandates.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SipMandateDto> dueMandates() {
        return sipRepository.findByStatus(SipStatus.ACTIVE).stream()
                .filter(m -> m.getNextInstalmentDate() != null
                        && !m.getNextInstalmentDate().isAfter(LocalDate.now()))
                .map(this::toDto).toList();
    }

    /** Executes one SIP instalment: places and allots a subscription, then advances the schedule. */
    @Transactional
    public SipMandateDto runInstalment(Long id) {
        SipMandate mandate = sipRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SipMandate", id));
        if (mandate.getStatus() != SipStatus.ACTIVE) {
            throw new BusinessException("SIP mandate is " + mandate.getStatus() + "; cannot run instalment");
        }

        transactionService.placeAndAllotSipInstalment(mandate);

        int executed = mandate.getInstalmentsExecuted() + 1;
        mandate.setInstalmentsExecuted(executed);
        mandate.setNextInstalmentDate(nextDate(mandate));
        if (mandate.getInstalmentCount() != null && executed >= mandate.getInstalmentCount()) {
            mandate.setStatus(SipStatus.COMPLETED);
        } else if (mandate.getEndDate() != null && mandate.getNextInstalmentDate().isAfter(mandate.getEndDate())) {
            mandate.setStatus(SipStatus.COMPLETED);
        }
        mandate = sipRepository.save(mandate);

        notificationService.notify(mandate.getInvestorId(), NotificationCategory.SIP,
                "SIP instalment " + executed + " for " + mandate.getMandateRef() + " processed");
        auditService.record("SIP_INSTALMENT", "SipMandate", mandate.getId(),
                "Instalment " + executed + " executed");
        return toDto(mandate);
    }

    @Transactional
    public SipMandateDto changeStatus(Long id, SipStatus status) {
        SipMandate mandate = sipRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SipMandate", id));
        mandate.setStatus(status);
        auditService.record("SIP_STATUS", "SipMandate", id, "Status set to " + status);
        return toDto(sipRepository.save(mandate));
    }

    private LocalDate nextDate(SipMandate mandate) {
        LocalDate base = mandate.getNextInstalmentDate() != null
                ? mandate.getNextInstalmentDate() : LocalDate.now();
        return mandate.getFrequency() == SipFrequency.QUARTERLY ? base.plusMonths(3) : base.plusMonths(1);
    }

    private SipMandateDto toDto(SipMandate mandate) {
        String schemeName = safeSchemeName(mandate.getOptionId());
        return mapper.toSipDto(mandate, schemeName);
    }

    private String safeSchemeName(Long optionId) {
        try {
            SchemeOptionDto option = fundCatalogClient.getOption(optionId);
            return option != null ? option.schemeName() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
