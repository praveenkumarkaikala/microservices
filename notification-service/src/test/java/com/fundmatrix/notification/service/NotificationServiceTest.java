package com.fundmatrix.notification.service;

import com.fundmatrix.notification.common.exception.ResourceNotFoundException;
import com.fundmatrix.notification.domain.Notification;
import com.fundmatrix.notification.domain.enums.NotificationCategory;
import com.fundmatrix.notification.domain.enums.NotificationStatus;
import com.fundmatrix.notification.dto.NotificationDto;
import com.fundmatrix.notification.repository.NotificationRepository;
import com.fundmatrix.notification.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserService currentUserService;

    private NotificationService notificationService;

    private static final Long CURRENT_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, currentUserService);
        lenient().when(currentUserService.getId()).thenReturn(CURRENT_USER_ID);
    }

    @Test
    void notify_createsNotificationForGivenUser() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notify(CURRENT_USER_ID, NotificationCategory.TRANSACTION, "Your order executed");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(CURRENT_USER_ID);
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.TRANSACTION);
        assertThat(saved.getMessage()).isEqualTo("Your order executed");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void notify_doesNothingWhenUserIdIsNull() {
        notificationService.notify(null, NotificationCategory.TRANSACTION, "no-op");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void myNotifications_returnsListForCurrentUser() {
        Notification n1 = Notification.builder()
                .userId(CURRENT_USER_ID).category(NotificationCategory.NAV)
                .message("NAV updated").status(NotificationStatus.UNREAD)
                .createdDate(Instant.now()).build();
        n1.setId(1L);

        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(CURRENT_USER_ID))
                .thenReturn(List.of(n1));

        List<NotificationDto> result = notificationService.myNotifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(result.get(0).message()).isEqualTo("NAV updated");
    }

    @Test
    void updateStatus_rejectsWhenNotificationBelongsToDifferentUser() {
        Notification n = Notification.builder()
                .userId(999L).category(NotificationCategory.KYC)
                .message("KYC approved").status(NotificationStatus.UNREAD)
                .createdDate(Instant.now()).build();
        n.setId(5L);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.updateStatus(5L, NotificationStatus.READ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllRead_updatesAllUnreadNotificationsForCurrentUser() {
        Notification n1 = Notification.builder()
                .userId(CURRENT_USER_ID).category(NotificationCategory.SIP)
                .message("SIP due").status(NotificationStatus.UNREAD)
                .createdDate(Instant.now()).build();
        n1.setId(1L);
        Notification n2 = Notification.builder()
                .userId(CURRENT_USER_ID).category(NotificationCategory.DIVIDEND)
                .message("Dividend declared").status(NotificationStatus.UNREAD)
                .createdDate(Instant.now()).build();
        n2.setId(2L);

        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                CURRENT_USER_ID, NotificationStatus.UNREAD)).thenReturn(List.of(n1, n2));

        notificationService.markAllRead();

        assertThat(n1.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(n2.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository, times(1)).saveAll(List.of(n1, n2));
    }
}
