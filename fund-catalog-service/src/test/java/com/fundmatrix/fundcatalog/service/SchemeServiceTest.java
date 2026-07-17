package com.fundmatrix.fundcatalog.service;

import com.fundmatrix.fundcatalog.common.exception.BusinessException;
import com.fundmatrix.fundcatalog.common.exception.ResourceNotFoundException;
import com.fundmatrix.fundcatalog.domain.FundScheme;
import com.fundmatrix.fundcatalog.domain.SchemeOption;
import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.OptionType;
import com.fundmatrix.fundcatalog.domain.enums.RiskProfile;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;
import com.fundmatrix.fundcatalog.dto.FundSchemeDto;
import com.fundmatrix.fundcatalog.dto.SaveOptionRequest;
import com.fundmatrix.fundcatalog.dto.SaveSchemeRequest;
import com.fundmatrix.fundcatalog.dto.SchemeOptionDto;
import com.fundmatrix.fundcatalog.repository.FundSchemeRepository;
import com.fundmatrix.fundcatalog.repository.SchemeOptionRepository;
import com.fundmatrix.fundcatalog.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemeServiceTest {

    @Mock
    private FundSchemeRepository schemeRepository;
    @Mock
    private SchemeOptionRepository optionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    @InjectMocks
    private SchemeService schemeService;

    private FundScheme scheme;

    @BeforeEach
    void setUp() {
        scheme = FundScheme.builder()
                .schemeName("Alpha Growth Fund")
                .schemeCode("ALPHA01")
                .category(SchemeCategory.EQUITY)
                .riskProfile(RiskProfile.HIGH)
                .status(SchemeStatus.ACTIVE)
                .build();
        scheme.setId(1L);
    }

    private SaveSchemeRequest sampleRequest() {
        return new SaveSchemeRequest(
                "Alpha Growth Fund", "ALPHA01", SchemeCategory.EQUITY, RiskProfile.HIGH,
                "NIFTY 50", 10L, "Jane Manager", BigDecimal.valueOf(5000),
                "1% within 365 days", BigDecimal.valueOf(1), 365, BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(500), BigDecimal.valueOf(500), "15:00", SchemeStatus.ACTIVE);
    }

    @Test
    void create_savesNewScheme_whenSchemeCodeUnique() {
        when(schemeRepository.existsBySchemeCodeIgnoreCase("ALPHA01")).thenReturn(false);
        when(schemeRepository.save(any(FundScheme.class))).thenReturn(scheme);
        when(optionRepository.findByScheme_Id(1L)).thenReturn(List.of());

        FundSchemeDto dto = schemeService.create(sampleRequest());

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.schemeName()).isEqualTo("Alpha Growth Fund");
        verify(auditService, times(1)).record(anyString(), anyString(), any(), anyString());
    }

    @Test
    void create_throwsBusinessException_whenSchemeCodeAlreadyExists() {
        when(schemeRepository.existsBySchemeCodeIgnoreCase("ALPHA01")).thenReturn(true);

        assertThatThrownBy(() -> schemeService.create(sampleRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ALPHA01");

        verify(schemeRepository, never()).save(any());
        verify(auditService, never()).record(anyString(), anyString(), any(), anyString());
    }

    @Test
    void addOption_savesOption_whenIsinUnique() {
        SchemeOption saved = SchemeOption.builder()
                .scheme(scheme)
                .optionType(OptionType.GROWTH)
                .isin("INF123456789")
                .status(OptionStatus.ACTIVE)
                .build();
        saved.setId(5L);

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(optionRepository.existsByIsinIgnoreCase("INF123456789")).thenReturn(false);
        when(optionRepository.save(any(SchemeOption.class))).thenReturn(saved);

        SchemeOptionDto dto = schemeService.addOption(1L,
                new SaveOptionRequest(OptionType.GROWTH, "INF123456789", OptionStatus.ACTIVE));

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.schemeId()).isEqualTo(1L);
        assertThat(dto.optionType()).isEqualTo(OptionType.GROWTH);
        verify(auditService, times(1)).record(anyString(), anyString(), any(), anyString());
    }

    @Test
    void addOption_throwsBusinessException_whenIsinAlreadyExists() {
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(optionRepository.existsByIsinIgnoreCase("INF123456789")).thenReturn(true);

        assertThatThrownBy(() -> schemeService.addOption(1L,
                new SaveOptionRequest(OptionType.GROWTH, "INF123456789", OptionStatus.ACTIVE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INF123456789");

        verify(optionRepository, never()).save(any());
    }

    @Test
    void list_returnsAllSchemesMappedToDto() {
        when(schemeRepository.findAll()).thenReturn(List.of(scheme));
        when(optionRepository.findByScheme_Id(1L)).thenReturn(List.of());

        List<FundSchemeDto> result = schemeService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schemeCode()).isEqualTo("ALPHA01");
    }

    @Test
    void get_throwsResourceNotFoundException_whenSchemeMissing() {
        when(schemeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schemeService.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_appliesChangesAndSaves_whenSchemeExists() {
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(schemeRepository.save(any(FundScheme.class))).thenReturn(scheme);
        when(optionRepository.findByScheme_Id(1L)).thenReturn(List.of());

        SaveSchemeRequest req = new SaveSchemeRequest(
                "Alpha Growth Fund Updated", "ALPHA01", SchemeCategory.EQUITY, RiskProfile.MODERATE,
                "NIFTY 50", 10L, "Jane Manager", BigDecimal.valueOf(6000),
                "1% within 365 days", BigDecimal.valueOf(1), 365, BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(500), BigDecimal.valueOf(500), "15:00", SchemeStatus.ACTIVE);

        FundSchemeDto dto = schemeService.update(1L, req);

        assertThat(dto).isNotNull();
        assertThat(scheme.getSchemeName()).isEqualTo("Alpha Growth Fund Updated");
        assertThat(scheme.getRiskProfile()).isEqualTo(RiskProfile.MODERATE);
        verify(schemeRepository, times(1)).save(scheme);
        verify(auditService, times(1)).record(anyString(), anyString(), any(), anyString());
    }

    @Test
    void update_throwsResourceNotFoundException_whenSchemeMissing() {
        when(schemeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schemeService.update(1L, sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
