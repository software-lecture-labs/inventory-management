package com.inventory.service;

import com.inventory.dto.ReservationDtos.ReservationRequest;
import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.dto.ReservationDtos.ReturnRequest;
import com.inventory.exception.BusinessException;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.model.WaitlistEntry;
import com.inventory.repository.ReservationRepository;
import com.inventory.repository.WaitlistRepository;
import com.inventory.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    // Testlerde "şu an" her zaman 2030-01-01 09:00
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 9, 0);

    private ReservationRepository reservationRepository;
    private WaitlistRepository waitlistRepository;
    private EquipmentService equipmentService;
    private UserService userService;
    private NotificationService notificationService;
    private ReservationService reservationService;

    private Equipment equipment;
    private User user;
    private UserPrincipal owner;
    private UserPrincipal otherUser;
    private UserPrincipal admin;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        waitlistRepository = mock(WaitlistRepository.class);
        equipmentService = mock(EquipmentService.class);
        userService = mock(UserService.class);
        notificationService = mock(NotificationService.class);
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(NOW.atZone(zone).toInstant(), zone);
        reservationService = new ReservationService(reservationRepository, waitlistRepository, equipmentService,
                userService, mock(AuditLogService.class), notificationService, clock, 30);

        equipment = new Equipment();
        equipment.setId(10L);
        equipment.setName("Osiloskop");
        equipment.setSerialNumber("SN-001");

        user = new User();
        user.setId(1L);
        user.setFirstName("Ali");
        user.setLastName("Yılmaz");

        owner = new UserPrincipal(1L, "ali@test.com", "Ali Yılmaz", "x", Role.USER, true, false);
        otherUser = new UserPrincipal(2L, "veli@test.com", "Veli Demir", "x", Role.USER, true, false);
        admin = new UserPrincipal(99L, "admin@test.com", "Admin", "x", Role.ADMIN, true, false);
    }

    private void slotIsFree() {
        when(equipmentService.findActive(10L)).thenReturn(equipment);
        when(userService.getById(1L)).thenReturn(user);
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Reservation reservation(ReservationStatus status, LocalDateTime start, LocalDateTime end) {
        Reservation r = new Reservation(equipment, user, start, end);
        r.setStatus(status);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        return r;
    }

    // ---------- oluşturma ----------

    @Test
    void createsApprovedReservationWhenTimeSlotIsFree() {
        slotIsFree();

        ReservationResponse response = reservationService.create(
                new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(3)), owner);

        assertThat(response.equipmentName()).isEqualTo("Osiloskop");
        assertThat(response.userFullName()).isEqualTo("Ali Yılmaz");
        assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
        verify(notificationService).reservationCreated(any());
    }

    @Test
    void reservationIsPendingWhenEquipmentRequiresApproval() {
        equipment.setRequiresApproval(true);
        slotIsFree();

        ReservationResponse response = reservationService.create(
                new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(3)), owner);

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void adminReservationDoesNotNeedApproval() {
        equipment.setRequiresApproval(true);
        slotIsFree();
        when(userService.getById(99L)).thenReturn(user);

        ReservationResponse response = reservationService.create(
                new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(3)), admin);

        assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void rejectsReservationForUnavailableEquipment() {
        equipment.setStatus(EquipmentStatus.OUT_OF_ORDER);
        when(equipmentService.findActive(10L)).thenReturn(equipment);

        assertThatThrownBy(() -> reservationService.create(
                new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(2)), owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Arızalı");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        ReservationRequest request = new ReservationRequest(10L, NOW.plusHours(3), NOW.plusHours(1));

        assertThatThrownBy(() -> reservationService.create(request, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bitiş zamanı");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectsReservationInThePast() {
        ReservationRequest request = new ReservationRequest(10L, NOW.minusHours(2), NOW.plusHours(1));

        assertThatThrownBy(() -> reservationService.create(request, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Geçmiş");
    }

    @Test
    void rejectsOverlappingReservation() {
        when(equipmentService.findActive(10L)).thenReturn(equipment);
        when(reservationRepository.existsOverlapping(eq(10L), any(), any(), any())).thenReturn(true);
        ReservationRequest request = new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(2));

        assertThatThrownBy(() -> reservationService.create(request, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten rezerve");
        verify(reservationRepository, never()).save(any());
    }

    // ---------- iptal / onay ----------

    @Test
    void userCannotCancelSomeoneElsesReservation() {
        Reservation r = reservation(ReservationStatus.APPROVED, NOW.plusHours(1), NOW.plusHours(2));

        assertThatThrownBy(() -> reservationService.cancel(5L, otherUser, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void adminCanCancelAnyReservationAndOwnerIsNotified() {
        Reservation r = reservation(ReservationStatus.APPROVED, NOW.plusHours(1), NOW.plusHours(2));

        reservationService.cancel(5L, admin, "Bakım planlandı");

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(r.getNote()).isEqualTo("Bakım planlandı");
        verify(notificationService).reservationCancelledByOther(r, "Admin");
    }

    @Test
    void checkedOutReservationCannotBeCancelled() {
        reservation(ReservationStatus.CHECKED_OUT, NOW.minusHours(1), NOW.plusHours(2));

        assertThatThrownBy(() -> reservationService.cancel(5L, owner, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("iade");
    }

    @Test
    void adminApprovesPendingReservation() {
        Reservation r = reservation(ReservationStatus.PENDING, NOW.plusHours(1), NOW.plusHours(2));

        reservationService.approve(5L, admin);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        verify(notificationService).reservationApproved(r);
    }

    @Test
    void onlyPendingReservationsCanBeApproved() {
        reservation(ReservationStatus.CANCELLED, NOW.plusHours(1), NOW.plusHours(2));

        assertThatThrownBy(() -> reservationService.approve(5L, admin))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- teslim alma / iade ----------

    @Test
    void checkOutIsNotAllowedTooEarly() {
        reservation(ReservationStatus.APPROVED, NOW.plusHours(2), NOW.plusHours(3));

        assertThatThrownBy(() -> reservationService.checkOut(5L, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("30 dakika");
    }

    @Test
    void checkOutWithinWindowSucceeds() {
        Reservation r = reservation(ReservationStatus.APPROVED, NOW.plusMinutes(20), NOW.plusHours(2));

        ReservationResponse response = reservationService.checkOut(5L, owner);

        assertThat(response.status()).isEqualTo(ReservationStatus.CHECKED_OUT);
        assertThat(r.getCheckedOutAt()).isEqualTo(NOW);
    }

    @Test
    void pendingReservationCannotBeCheckedOut() {
        reservation(ReservationStatus.PENDING, NOW, NOW.plusHours(2));

        assertThatThrownBy(() -> reservationService.checkOut(5L, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("onaylanmadı");
    }

    @Test
    void lateReturnIsDetectedAndFaultMarksEquipmentOutOfOrder() {
        Reservation r = reservation(ReservationStatus.CHECKED_OUT, NOW.minusHours(3), NOW.minusHours(1));

        ReservationResponse response = reservationService.returnEquipment(5L, owner,
                new ReturnRequest("Ekran yanmıyor", true));

        assertThat(response.status()).isEqualTo(ReservationStatus.RETURNED);
        assertThat(response.returnedLate()).isTrue();
        assertThat(r.getReturnedAt()).isEqualTo(NOW);
        verify(equipmentService).changeStatus(equipment, EquipmentStatus.OUT_OF_ORDER, "Ekran yanmıyor", "Ali Yılmaz");
    }

    @Test
    void overdueFlagIsSetForCheckedOutReservationPastEndTime() {
        Reservation r = new Reservation(equipment, user, NOW.minusHours(3), NOW.minusHours(1));
        r.setStatus(ReservationStatus.CHECKED_OUT);

        assertThat(ReservationResponse.from(r, NOW).overdue()).isTrue();
    }

    // ---------- bekleme listesi ----------

    @Test
    void waitlistIsOnlyForBusySlots() {
        when(equipmentService.findActive(10L)).thenReturn(equipment);
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> reservationService.joinWaitlist(
                new ReservationRequest(10L, NOW.plusHours(1), NOW.plusHours(2)), owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("boş");
    }

    @Test
    void cancellingNotifiesFirstWaitingUserWhoseSlotIsNowFree() {
        Reservation r = reservation(ReservationStatus.APPROVED, NOW.plusHours(1), NOW.plusHours(3));
        equipment.setActive(true);

        User waiting = new User();
        waiting.setId(3L);
        WaitlistEntry first = new WaitlistEntry(equipment, waiting, NOW.plusHours(1), NOW.plusHours(2));
        WaitlistEntry clashing = new WaitlistEntry(equipment, waiting, NOW.plusMinutes(90), NOW.plusHours(3));
        when(waitlistRepository.findWaiting(eq(10L), any(), any(), any())).thenReturn(List.of(first, clashing));
        when(reservationRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);

        reservationService.cancel(5L, owner, null);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        // Haber verilen kişi bekleme listesinden çıkarılır
        verify(notificationService).waitlistSlotAvailable(first);
        verify(waitlistRepository).delete(first);
        // İlk bildirilenle çakıştığı için ikinci kişiye aynı anda haber verilmez, listede kalır
        verify(notificationService, never()).waitlistSlotAvailable(clashing);
        verify(waitlistRepository, never()).delete(clashing);
    }

    @Test
    void reservingRemovesOwnOverlappingWaitlistEntries() {
        slotIsFree();
        LocalDateTime start = NOW.plusHours(1);
        LocalDateTime end = NOW.plusHours(3);

        reservationService.create(new ReservationRequest(10L, start, end), owner);

        verify(waitlistRepository).deleteForUserOverlapping(10L, 1L, start, end);
    }
}
