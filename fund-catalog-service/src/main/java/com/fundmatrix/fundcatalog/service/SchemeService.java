package com.fundmatrix.fundcatalog.service;

import com.fundmatrix.fundcatalog.common.exception.BusinessException;
import com.fundmatrix.fundcatalog.common.exception.ResourceNotFoundException;
import com.fundmatrix.fundcatalog.domain.FundScheme;
import com.fundmatrix.fundcatalog.domain.SchemeOption;
import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;
import com.fundmatrix.fundcatalog.dto.FundSchemeDto;
import com.fundmatrix.fundcatalog.dto.SaveOptionRequest;
import com.fundmatrix.fundcatalog.dto.SaveSchemeRequest;
import com.fundmatrix.fundcatalog.dto.SchemeOptionDto;
import com.fundmatrix.fundcatalog.dto.response.SchemeOptionDetailDto;
import com.fundmatrix.fundcatalog.repository.FundSchemeRepository;
import com.fundmatrix.fundcatalog.repository.SchemeOptionRepository;
import com.fundmatrix.fundcatalog.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fund scheme catalogue and scheme-option administration.
 *
 * NOTE on NavRecord: the monolith's version injected NavRecordRepository purely to attach a
 * convenience "latestNav" field onto each SchemeOptionDto. NavRecord/NavRecordRepository now
 * live in nav-accounting-service. Rather than fan out one Feign call per option on every plain
 * catalogue read (list/get), that field has been dropped from this module's DTOs - see the
 * comment on FundSchemeDto. Business logic here is otherwise unchanged from the monolith.
 */
@Service
public class SchemeService {

    private final FundSchemeRepository schemeRepository;
    private final SchemeOptionRepository optionRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public SchemeService(FundSchemeRepository schemeRepository, SchemeOptionRepository optionRepository,
                         AuditService auditService, CurrentUserService currentUser) {
        this.schemeRepository = schemeRepository;
        this.optionRepository = optionRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public FundSchemeDto create(SaveSchemeRequest req) {
        if (schemeRepository.existsBySchemeCodeIgnoreCase(req.schemeCode())) {
            throw new BusinessException("Scheme code " + req.schemeCode() + " already exists");
        }
        FundScheme scheme = new FundScheme();
        apply(scheme, req);
        scheme.setStatus(req.status() != null ? req.status() : SchemeStatus.ACTIVE);
        scheme = schemeRepository.save(scheme);
        auditService.record("SCHEME_CREATE", "FundScheme", scheme.getId(), "Created " + scheme.getSchemeName());
        return toDto(scheme);
    }

    @Transactional
    public FundSchemeDto update(Long id, SaveSchemeRequest req) {
        FundScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", id));
        if (!scheme.getSchemeCode().equalsIgnoreCase(req.schemeCode())
                && schemeRepository.existsBySchemeCodeIgnoreCase(req.schemeCode())) {
            throw new BusinessException("Scheme code " + req.schemeCode() + " already exists");
        }
        apply(scheme, req);
        if (req.status() != null) {
            scheme.setStatus(req.status());
        }
        auditService.record("SCHEME_UPDATE", "FundScheme", id, "Updated " + scheme.getSchemeName());
        return toDto(schemeRepository.save(scheme));
    }

    @Transactional(readOnly = true)
    public List<FundSchemeDto> list() {
        return schemeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FundSchemeDto get(Long id) {
        return toDto(schemeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", id)));
    }

    @Transactional
    public SchemeOptionDto addOption(Long schemeId, SaveOptionRequest req) {
        FundScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", schemeId));
        if (req.isin() != null && !req.isin().isBlank()
                && optionRepository.existsByIsinIgnoreCase(req.isin())) {
            throw new BusinessException("ISIN " + req.isin() + " already exists");
        }
        SchemeOption option = SchemeOption.builder()
                .scheme(scheme)
                .optionType(req.optionType())
                .isin(req.isin())
                .status(req.status() != null ? req.status() : OptionStatus.ACTIVE)
                .build();
        option = optionRepository.save(option);
        auditService.record("OPTION_CREATE", "SchemeOption", option.getId(),
                req.optionType() + " option for scheme " + scheme.getSchemeName());
        return toOptionDto(option);
    }

    @Transactional(readOnly = true)
    public List<SchemeOptionDto> listOptions(Long schemeId) {
        return optionRepository.findByScheme_Id(schemeId).stream()
                .map(this::toOptionDto).toList();
    }

    /** Service-to-service lookup backing GET /schemes/options/{optionId} (FEIGN_CONTRACTS.md). */
    @Transactional(readOnly = true)
    public SchemeOptionDetailDto getOptionDetail(Long optionId) {
        SchemeOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", optionId));
        FundScheme scheme = option.getScheme();
        return new SchemeOptionDetailDto(
                option.getId(), scheme.getId(), scheme.getSchemeName(), option.getOptionType(),
                option.getIsin(), option.getStatus(), scheme.getStatus(), scheme.getCategory(),
                scheme.getCutoffTime(), scheme.getExitLoadRate(), scheme.getMinInvestment());
    }

    private void apply(FundScheme scheme, SaveSchemeRequest req) {
        scheme.setSchemeName(req.schemeName());
        scheme.setSchemeCode(req.schemeCode());
        scheme.setCategory(req.category());
        scheme.setRiskProfile(req.riskProfile());
        scheme.setBenchmarkIndex(req.benchmarkIndex());
        scheme.setFundManagerId(req.fundManagerId());
        scheme.setFundManagerName(req.fundManagerName());
        scheme.setMinInvestment(req.minInvestment());
        scheme.setExitLoadSlab(req.exitLoadSlab());
        scheme.setExitLoadRate(req.exitLoadRate());
        scheme.setExitLoadPeriodDays(req.exitLoadPeriodDays());
        scheme.setExpenseRatio(req.expenseRatio());
        scheme.setMinSipAmount(req.minSipAmount());
        scheme.setMinSwpAmount(req.minSwpAmount());
        scheme.setCutoffTime(req.cutoffTime());
    }

    private FundSchemeDto toDto(FundScheme scheme) {
        List<SchemeOptionDto> options = optionRepository.findByScheme_Id(scheme.getId()).stream()
                .map(this::toOptionDto).toList();
        return new FundSchemeDto(scheme.getId(), scheme.getSchemeName(), scheme.getSchemeCode(),
                scheme.getCategory(), scheme.getRiskProfile(), scheme.getBenchmarkIndex(),
                scheme.getFundManagerId(), scheme.getFundManagerName(), scheme.getMinInvestment(),
                scheme.getExitLoadSlab(), scheme.getExitLoadRate(), scheme.getExitLoadPeriodDays(),
                scheme.getExpenseRatio(), scheme.getMinSipAmount(), scheme.getMinSwpAmount(),
                scheme.getCutoffTime(), scheme.getStatus(), options);
    }

    private SchemeOptionDto toOptionDto(SchemeOption o) {
        return new SchemeOptionDto(o.getId(), o.getScheme().getId(), o.getScheme().getSchemeName(),
                o.getOptionType(), o.getIsin(), o.getStatus());
    }
}
