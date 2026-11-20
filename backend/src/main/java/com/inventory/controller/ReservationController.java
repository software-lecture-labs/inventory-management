package com.inventory.controller;

import com.inventory.dto.PageResponse;
import com.inventory.dto.ReservationDtos.NoteRequest;
import com.inventory.dto.ReservationDtos.ReservationRequest;
import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.dto.ReservationDtos.ReturnRequest;
import com.inventory.model.ReservationStatus;
import com.inventory.security.UserPrincipal;
import com.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Takvim: verilen zaman aralığıyla kesişen rezervasyonlar (iptal / red hariç). */
    @GetMapping
    public List<ReservationResponse> calendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return reservationService.getCalendar(from, to);
    }

    /**
     * Sayfalı rezervasyon listesi. Yönetici herkesinkini görür;
     * normal kullanıcı sadece kendi rezervasyonlarını görür.
     */
    @GetMapping("/search")
    public PageResponse<ReservationResponse> search(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (mine || !principal.isAdmin()) ? principal.id() : null;
        var filter = new ReservationService.Filter(userId, user, equipment, status, from, to, overdue);
        return reservationService.search(filter, Paging.page(page), Paging.size(size));
    }

    // Rezervasyonu yapan kişi token'dan alınır, istemciden gelmez
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.create(request, principal);
    }

    // approve / reject sadece ADMIN (SecurityConfig)
    @PostMapping("/{id}/approve")
    public ReservationResponse approve(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.approve(id, principal);
    }

    @PostMapping("/{id}/reject")
    public ReservationResponse reject(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) NoteRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.reject(id, request == null ? null : request.note(), principal);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) NoteRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.cancel(id, principal, request == null ? null : request.note());
    }

    @PostMapping("/{id}/check-out")
    public ReservationResponse checkOut(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.checkOut(id, principal);
    }

    @PostMapping("/{id}/return")
    public ReservationResponse returnEquipment(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) ReturnRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.returnEquipment(id, principal, request);
    }
}
