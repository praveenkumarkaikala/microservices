package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.AuthUserClient;
import com.fundmatrix.foliokyc.client.FundCatalogClient;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import com.fundmatrix.foliokyc.domain.enums.ModeOfHolding;
import com.fundmatrix.foliokyc.domain.enums.Role;
import com.fundmatrix.foliokyc.domain.enums.TaxStatus;
import com.fundmatrix.foliokyc.dto.CreateFolioRequest;
import com.fundmatrix.foliokyc.dto.FolioDto;
import com.fundmatrix.foliokyc.dto.UserDto;
import com.fundmatrix.foliokyc.repository.FolioHoldingRepository;
import com.fundmatrix.foliokyc.repository.InvestorFolioRepository;
import com.fundmatrix.foliokyc.repository.KycRecordRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolioServiceTest {

    @Mock
    private InvestorFolioRepository folioRepository;
    @Mock
    private FolioHoldingRepository holdingRepository;
    @Mock
    private AuthUserClient authUserClient;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private HoldingService holdingService;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;
    
    @Mock
    private KycRecordRepository kycRecordRepository;
    private FolioService folioService;

    @BeforeEach
    void setUp() {
        folioService = new FolioService(folioRepository, holdingRepository, authUserClient,
                fundCatalogClient, holdingService, auditService, currentUser, new Mapper(),kycRecordRepository);
        lenient().when(holdingRepository.findByFolio_Id(any())).thenReturn(List.of());
    }

    @Test
    void create_forInvestorRole_usesCurrentUserAsInvestor() {
        when(currentUser.getRole()).thenReturn(Role.INVESTOR);
        when(currentUser.getId()).thenReturn(42L);
        when(authUserClient.getUser(42L)).thenReturn(new UserDto(42L, "Jane Doe", "jane@x.com", "INVESTOR", "ACTIVE"));
        when(folioRepository.save(any())).thenAnswer(inv -> {
            InvestorFolio f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(7L);
            }
            return f;
        });

        CreateFolioRequest req = new CreateFolioRequest(null, null, TaxStatus.INDIVIDUAL,
                ModeOfHolding.SINGLE, null, null);
        FolioDto dto = folioService.create(req);

        assertThat(dto.investorId()).isEqualTo(42L);
        assertThat(dto.investorName()).isEqualTo("Jane Doe");
        assertThat(dto.folioNumber()).isEqualTo("FOL00007");
        assertThat(dto.status()).isEqualTo(FolioStatus.ACTIVE);
    }

    @Test
    void loadAccessible_investorCannotAccessAnotherInvestorsFolio() {
        InvestorFolio folio = InvestorFolio.builder().build();
        folio.setId(1L);
        folio.setInvestorId(99L);
        when(folioRepository.findById(1L)).thenReturn(Optional.of(folio));
        when(currentUser.getRole()).thenReturn(Role.INVESTOR);
        when(currentUser.getId()).thenReturn(1L);

        assertThatThrownBy(() -> folioService.loadAccessible(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadAccessible_investorCanAccessOwnFolio() {
        InvestorFolio folio = InvestorFolio.builder().build();
        folio.setId(1L);
        folio.setInvestorId(1L);
        when(folioRepository.findById(1L)).thenReturn(Optional.of(folio));
        when(currentUser.getRole()).thenReturn(Role.INVESTOR);
        when(currentUser.getId()).thenReturn(1L);

        InvestorFolio result = folioService.loadAccessible(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }
}
