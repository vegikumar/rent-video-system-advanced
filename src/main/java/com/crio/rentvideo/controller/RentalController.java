package com.crio.rentvideo.controller;

import com.crio.rentvideo.dto.RentalResponse;
import com.crio.rentvideo.service.RentalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping("/api/videos/{videoId}/rent")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<RentalResponse> rentVideo(
            @PathVariable Long videoId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalService.rentVideo(videoId, authentication));
    }

    @PostMapping("/api/videos/{videoId}/return")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<RentalResponse> returnVideo(
            @PathVariable Long videoId, Authentication authentication) {
        return ResponseEntity.ok(rentalService.returnVideo(videoId, authentication));
    }

    /**
     * Alias for the assignment's stated return path (books → videos typo in prompt).
     */
    @PostMapping("/api/books/{videoId}/return")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<RentalResponse> returnVideoAlias(
            @PathVariable Long videoId, Authentication authentication) {
        return ResponseEntity.ok(rentalService.returnVideo(videoId, authentication));
    }

    @GetMapping("/api/rentals/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<RentalResponse>> myActiveRentals(Authentication authentication) {
        return ResponseEntity.ok(rentalService.getMyActiveRentals(authentication));
    }
}
