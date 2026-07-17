package com.fundmatrix.distributorcommission.service;

import com.fundmatrix.distributorcommission.client.AuthUserClient;
import com.fundmatrix.distributorcommission.client.FolioKycClient;
import com.fundmatrix.distributorcommission.client.UserDto;
import com.fundmatrix.distributorcommission.common.exception.BusinessException;
import com.fundmatrix.distributorcommission.common.exception.ResourceNotFoundException;
import com.fundmatrix.distributorcommission.domain.Distributor;
import com.fundmatrix.distributorcommission.domain.enums.CommissionModel;
import com.fundmatrix.distributorcommission.domain.enums.DistributorStatus;
import com.fundmatrix.distributorcommission.dto.DistributorDto;
import com.fundmatrix.distributorcommission.dto.SaveDistributorRequest;
import com.fundmatrix.distributorcommission.repository.DistributorRepository;
import com.fundmatrix.distributorcommission.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributorServiceTest {

    @Mock
    private DistributorRepository distributorRepository;
    @Mock
    private AuthUserClient authUserClient;
    @Mock
    private FolioKycClient folioTransactionClient;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    @InjectMocks
    private DistributorService distributorService;

    @Test
    void create_withLinkedDistributorUser_populatesUserFieldsFromFeign() {
        SaveDistributorRequest req = new SaveDistributorRequest("Acme Distributors", "ARN999", "EUIN1",
                LocalDate.of(2020, 1, 1), CommissionModel.TRAIL, DistributorStatus.ACTIVE, 42L);
        UserDto userDto = new UserDto(42L, "Jane Doe", "jane@example.com", "999", "DISTRIBUTOR", "ACTIVE", null);

        lenient().when(distributorRepository.existsByArnNumberIgnoreCase("ARN999")).thenReturn(false);
        when(authUserClient.getUser(42L)).thenReturn(userDto);
        when(distributorRepository.save(any(Distributor.class))).thenAnswer(inv -> {
            Distributor d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        lenient().when(folioTransactionClient.aumForDistributor(eq(1L), isNull()))
                .thenReturn(new BigDecimal("0.00"));

        DistributorDto dto = distributorService.create(req);

        assertThat(dto.userId()).isEqualTo(42L);
        assertThat(dto.userName()).isEqualTo("Jane Doe");
        assertThat(dto.userEmail()).isEqualTo("jane@example.com");
        verify(auditService).record(eq("DISTRIBUTOR_CREATE"), eq("Distributor"), any(), any());
    }

    @Test
    void create_linkedUserNotDistributorRole_throwsBusinessException() {
        SaveDistributorRequest req = new SaveDistributorRequest("Acme", null, null,
                null, CommissionModel.TRAIL, null, 7L);
        UserDto userDto = new UserDto(7L, "Bob", "bob@example.com", null, "ADMIN", "ACTIVE", null);
        when(authUserClient.getUser(7L)).thenReturn(userDto);

        assertThatThrownBy(() -> distributorService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DISTRIBUTOR role");
    }

    @Test
    void update_distributorNotFound_throwsResourceNotFound() {
        SaveDistributorRequest req = new SaveDistributorRequest("Acme", null, null,
                null, CommissionModel.TRAIL, null, null);
        when(distributorRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> distributorService.update(5L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireForCurrentUser_usesRenamedFindByUserId() {
        Distributor d = Distributor.builder().name("Acme").commissionModel(CommissionModel.TRAIL)
                .status(DistributorStatus.ACTIVE).userId(42L).build();
        d.setId(1L);
        when(currentUser.getId()).thenReturn(42L);
        when(distributorRepository.findByUserId(42L)).thenReturn(Optional.of(d));

        Distributor result = distributorService.requireForCurrentUser();

        assertThat(result.getId()).isEqualTo(1L);
        verify(distributorRepository).findByUserId(42L);
    }

    @Test
    void requireForCurrentUser_noLinkedDistributor_throwsBusinessException() {
        when(currentUser.getId()).thenReturn(999L);
        when(distributorRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> distributorService.requireForCurrentUser())
                .isInstanceOf(BusinessException.class);
    }
}
