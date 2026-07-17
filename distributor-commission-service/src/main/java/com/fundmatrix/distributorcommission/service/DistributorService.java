package com.fundmatrix.distributorcommission.service;

import com.fundmatrix.distributorcommission.client.AuthUserClient;
import com.fundmatrix.distributorcommission.client.UserDto;
import com.fundmatrix.distributorcommission.common.Calc;
import com.fundmatrix.distributorcommission.common.exception.BusinessException;
import com.fundmatrix.distributorcommission.common.exception.ResourceNotFoundException;
import com.fundmatrix.distributorcommission.domain.Distributor;
import com.fundmatrix.distributorcommission.domain.enums.DistributorStatus;
import com.fundmatrix.distributorcommission.dto.DistributorDto;
import com.fundmatrix.distributorcommission.dto.SaveDistributorRequest;
import com.fundmatrix.distributorcommission.client.FolioKycClient;
import com.fundmatrix.distributorcommission.repository.DistributorRepository;
import com.fundmatrix.distributorcommission.security.CurrentUserService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Distributor empanelment and lookup, with AUM enrichment sourced from folio-transaction-service. */
@Service
public class DistributorService {

    private static final Logger log = LoggerFactory.getLogger(DistributorService.class);

    private final DistributorRepository distributorRepository;
    private final AuthUserClient authUserClient;
    private final FolioKycClient folioTransactionClient;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public DistributorService(DistributorRepository distributorRepository, AuthUserClient authUserClient,
                              FolioKycClient folioTransactionClient, AuditService auditService,
                              CurrentUserService currentUser) {
        this.distributorRepository = distributorRepository;
        this.authUserClient = authUserClient;
        this.folioTransactionClient = folioTransactionClient;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public DistributorDto create(SaveDistributorRequest req) {
        if (req.arnNumber() != null && !req.arnNumber().isBlank()
                && distributorRepository.existsByArnNumberIgnoreCase(req.arnNumber())) {
            throw new BusinessException("A distributor with ARN " + req.arnNumber() + " already exists");
        }
        Distributor d = new Distributor();
        apply(d, req);
        d.setStatus(req.status() != null ? req.status() : DistributorStatus.ACTIVE);
        d = distributorRepository.save(d);
        auditService.record("DISTRIBUTOR_CREATE", "Distributor", d.getId(), "Empanelled " + d.getName());
        return toDto(d);
    }

    @Transactional
    public DistributorDto update(Long id, SaveDistributorRequest req) {
        Distributor d = distributorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", id));
        apply(d, req);
        if (req.status() != null) {
            d.setStatus(req.status());
        }
        auditService.record("DISTRIBUTOR_UPDATE", "Distributor", id, "Updated " + d.getName());
        return toDto(distributorRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DistributorDto> list() {
        return distributorRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DistributorDto get(Long id) {
        return toDto(distributorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", id)));
    }

    /** Resolves the distributor record linked to the authenticated distributor user. */
    @Transactional(readOnly = true)
    public Distributor requireForCurrentUser() {
        return distributorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(
                        "No distributor profile is linked to your account"));
    }

    private void apply(Distributor d, SaveDistributorRequest req) {
        d.setName(req.name());
        d.setArnNumber(req.arnNumber());
        d.setEuinNumber(req.euinNumber());
        d.setEmpanelmentDate(req.empanelmentDate());
        d.setCommissionModel(req.commissionModel());
        if (req.userId() != null) {
            UserDto user;
            try {
                user = authUserClient.getUser(req.userId());
            } catch (FeignException.NotFound ex) {
                throw ResourceNotFoundException.of("User", req.userId());
            } catch (FeignException ex) {
                throw new BusinessException("Unable to verify linked user " + req.userId() + ": " + ex.getMessage());
            }
            if (user == null) {
                throw ResourceNotFoundException.of("User", req.userId());
            }
            if (!"DISTRIBUTOR".equals(user.role())) {
                throw new BusinessException("Linked user must have the DISTRIBUTOR role");
            }
            d.setUserId(user.id());
        }
    }

    private DistributorDto toDto(Distributor d) {
        BigDecimal aum;
        try {
            aum = Calc.money(folioTransactionClient.aumForDistributor(d.getId(), null));
        } catch (FeignException ex) {
            log.warn("Unable to fetch AUM for distributor {}: {}", d.getId(), ex.getMessage());
            aum = null;
        }
        String userName = null;
        String userEmail = null;
        if (d.getUserId() != null) {
            try {
                UserDto user = authUserClient.getUser(d.getUserId());
                if (user != null) {
                    userName = user.name();
                    userEmail = user.email();
                }
            } catch (FeignException ex) {
                log.warn("Unable to fetch linked user {} for distributor {}: {}",
                        d.getUserId(), d.getId(), ex.getMessage());
            }
        }
        return new DistributorDto(d.getId(), d.getName(), d.getArnNumber(), d.getEuinNumber(),
                d.getEmpanelmentDate(), d.getCommissionModel(), d.getStatus(),
                d.getUserId(), userName, userEmail, aum);
    }
}
