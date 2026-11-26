package com.inventory.controller;

import com.inventory.dto.ReservationDtos.ReservationRequest;
import com.inventory.dto.ReservationDtos.WaitlistResponse;
import com.inventory.security.UserPrincipal;
import com.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final ReservationService reservationService;

    public WaitlistController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Oturumdaki kullanıcının bekleme listesi kayıtları. */
    @GetMapping
    public List<WaitlistResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.getMyWaitlist(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistResponse join(@Valid @RequestBody ReservationRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.joinWaitlist(request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        reservationService.leaveWaitlist(id, principal);
    }
}
