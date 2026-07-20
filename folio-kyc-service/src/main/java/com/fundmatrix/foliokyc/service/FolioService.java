package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.AuthUserClient;
import com.fundmatrix.foliokyc.client.FundCatalogClient;
import com.fundmatrix.foliokyc.common.Calc;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.FolioHolding;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import com.fundmatrix.foliokyc.domain.enums.Role;
import com.fundmatrix.foliokyc.dto.CreateFolioRequest;
import com.fundmatrix.foliokyc.dto.FolioDto;
import com.fundmatrix.foliokyc.dto.FolioHoldingDto;
import com.fundmatrix.foliokyc.dto.SchemeOptionDto;
import com.fundmatrix.foliokyc.dto.UserDto;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import com.fundmatrix.foliokyc.repository.KycRecordRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Investor folio lifecycle and holdings, with role-scoped visibility.
 *
 * KNOWN LIMITATION: distributor-commission-service (which owns Distributor) is not a
 * contracted Feign consumer of this service, so there is no way to resolve "which
 * Distributor record belongs to the current DISTRIBUTOR-role user" (that mapping lived on
 * Distributor.user in the monolith). This migration assumes the JWT-carried user id can be
 * used directly as distributorId for DISTRIBUTOR-role scoping; revisit once a
 * distributor-commission-service Feign contract is added for this service.
 *
 * Note: this class intentionally has no dependency on KycService/KycRecordRepository -
 * Folio and KYC merely live in the same module/schema now, they are not coupled.
 */
@Service
public class FolioService {

    private final InvestorFolioRepository folioRepository;
    private final FolioHoldingRepository holdingRepository;
    private final AuthUserClient authUserClient;
    private final FundCatalogClient fundCatalogClient;
    private final KycRecordRepository kycRepository;
    private final HoldingService holdingService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public FolioService(InvestorFolioRepository folioRepository, FolioHoldingRepository holdingRepository,
                        AuthUserClient authUserClient, FundCatalogClient fundCatalogClient,
                        HoldingService holdingService, AuditService auditService,
                        CurrentUserService currentUser, Mapper mapper,KycRecordRepository kycRepository) {
        this.folioRepository = folioRepository;
        this.holdingRepository = holdingRepository;
        this.authUserClient = authUserClient;
        this.fundCatalogClient = fundCatalogClient;
        this.holdingService = holdingService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.kycRepository=kycRepository;
    }

    @Transactional
    public FolioDto create(CreateFolioRequest req) {
        UserDto investor = resolveInvestor(req);
        Long distributorId = resolveDistributorId(req);
        
        KycRecord record=kycRepository.findByInvestorId(investor.id());
        
        if(record==null)
        {
        	throw new BusinessException("Kyc Not Verified");
        }
        
        InvestorFolio folio = InvestorFolio.builder()
                .investorId(investor.id())
                .distributorId(distributorId)
                .taxStatus(req.taxStatus())
                .modeOfHolding(req.modeOfHolding())
                .nomineeDetails(req.nomineeDetails())
                .bankAccountRef(req.bankAccountRef())
                .status(FolioStatus.ACTIVE)
                .build();
        folio = folioRepository.save(folio);
        folio.setFolioNumber(String.format("FOL%05d", folio.getId()));
        folio = folioRepository.save(folio);

        auditService.record("FOLIO_CREATE", "InvestorFolio", folio.getId(),
                "Folio " + folio.getFolioNumber() + " for investor " + investor.name());
        return toDto(folio, investor.name());
    }

    @Transactional(readOnly = true)
    public List<FolioDto> listForCurrentUser() {
        Role role = currentUser.getRole();
        List<InvestorFolio> folios = switch (role) {
            case INVESTOR -> folioRepository.findByInvestorId(currentUser.getId());
            // See class-level note: distributorId is assumed == the DISTRIBUTOR user's id.
            case DISTRIBUTOR -> folioRepository.findByDistributorId(currentUser.getId());
            default -> folioRepository.findAll();
        };
        return folios.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FolioDto get(Long id) {
        return toDto(loadAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<FolioHoldingDto> holdings(Long folioId) {
        loadAccessible(folioId);
        return holdingRepository.findByFolio_Id(folioId).stream()
                .map(h -> {
                    SchemeOptionDto option = fundCatalogClient.getOption(h.getOptionId());
                    BigDecimal latestNav = null;
                    return mapper.toHoldingDto(h, latestNav,
                            option != null ? option.schemeName() : null,
                            option != null ? option.optionType() : null);
                })
                .toList();
    }

    @Transactional
    public FolioDto updateStatus(Long id, FolioStatus status) {
        InvestorFolio folio = folioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("InvestorFolio", id));
        folio.setStatus(status);
        auditService.record("FOLIO_STATUS", "InvestorFolio", id, "Status set to " + status);
        return toDto(folioRepository.save(folio));
    }


    @Transactional(readOnly = true)
    public InvestorFolio loadAccessible(Long id) {
        InvestorFolio folio = folioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("InvestorFolio", id));
        Role role = currentUser.getRole();
        if (role == Role.INVESTOR && !folio.getInvestorId().equals(currentUser.getId())) {
            throw ResourceNotFoundException.of("InvestorFolio", id);
        }
        if (role == Role.DISTRIBUTOR) {
            // See class-level note: distributorId is assumed == the DISTRIBUTOR user's id.
            boolean owns = folio.getDistributorId() != null
                    && folio.getDistributorId().equals(currentUser.getId());
            if (!owns) {
                throw ResourceNotFoundException.of("InvestorFolio", id);
            }
        }
        return folio;
    }

    private UserDto resolveInvestor(CreateFolioRequest req) {
        if (currentUser.getRole() == Role.INVESTOR) {
            return authUserClient.getUser(currentUser.getId());
        }
        if (req.investorId() == null) {
            throw new BusinessException("investorId is required when creating a folio on behalf of an investor");
        }
        UserDto investor = authUserClient.getUser(req.investorId());
        if (investor == null) {
            throw ResourceNotFoundException.of("User", req.investorId());
        }
        if (!"INVESTOR".equals(investor.role())) {
            throw new BusinessException("Folios can only be created for users with the INVESTOR role");
        }
        return investor;
    }

    private Long resolveDistributorId(CreateFolioRequest req) {
        if (req.distributorId() != null) {
            return req.distributorId();
        }
        if (currentUser.getRole() == Role.DISTRIBUTOR) {
            // See class-level note: distributorId is assumed == the DISTRIBUTOR user's id.
            return currentUser.getId();
        }
        return null;
    }

    private FolioDto toDto(InvestorFolio folio) {
        String investorName = safeInvestorName(folio.getInvestorId());
        return toDto(folio, investorName);
    }

    private FolioDto toDto(InvestorFolio folio, String investorName) {
        BigDecimal currentValue = holdingRepository.findByFolio_Id(folio.getId()).stream()
                .map(FolioHolding::getCurrentValue).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return mapper.toFolioDto(folio, Calc.money(currentValue), investorName);
    }

    private String safeInvestorName(Long investorId) {
        try {
            UserDto user = authUserClient.getUser(investorId);
            return user != null ? user.name() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
