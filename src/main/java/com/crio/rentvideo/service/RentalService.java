package com.crio.rentvideo.service;

import com.crio.rentvideo.dto.RentalResponse;
import com.crio.rentvideo.entity.Rental;
import com.crio.rentvideo.entity.User;
import com.crio.rentvideo.entity.Video;
import com.crio.rentvideo.exception.BadRequestException;
import com.crio.rentvideo.exception.ResourceNotFoundException;
import com.crio.rentvideo.repository.RentalRepository;
import com.crio.rentvideo.repository.UserRepository;
import com.crio.rentvideo.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalService {

    private static final int MAX_ACTIVE_RENTALS = 2;

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final VideoService videoService;

    @Transactional
    public RentalResponse rentVideo(Long videoId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Video video = videoService.findVideo(videoId);

        if (!video.isAvailable()) {
            throw new BadRequestException("Video is not available for rent: " + video.getTitle());
        }

        if (rentalRepository.findByUserIdAndVideoIdAndActiveTrue(user.getId(), videoId).isPresent()) {
            throw new BadRequestException("You already have an active rental for this video");
        }

        long activeCount = rentalRepository.countByUserIdAndActiveTrue(user.getId());
        if (activeCount >= MAX_ACTIVE_RENTALS) {
            throw new BadRequestException(
                    "Maximum of " + MAX_ACTIVE_RENTALS + " active rentals allowed. Return a video first.");
        }

        video.setAvailable(false);

        Rental rental = Rental.builder()
                .user(user)
                .video(video)
                .rentedAt(LocalDateTime.now())
                .active(true)
                .build();

        return toResponse(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponse returnVideo(Long videoId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Video video = videoService.findVideo(videoId);

        Rental rental = rentalRepository
                .findByUserIdAndVideoIdAndActiveTrue(user.getId(), videoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active rental found for video id: " + videoId));

        rental.setActive(false);
        rental.setReturnedAt(LocalDateTime.now());
        video.setAvailable(true);

        return toResponse(rentalRepository.save(rental));
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getMyActiveRentals(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return rentalRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new BadRequestException("Authenticated user required");
        }
        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private RentalResponse toResponse(Rental rental) {
        return RentalResponse.builder()
                .id(rental.getId())
                .userId(rental.getUser().getId())
                .videoId(rental.getVideo().getId())
                .videoTitle(rental.getVideo().getTitle())
                .rentedAt(rental.getRentedAt())
                .returnedAt(rental.getReturnedAt())
                .active(rental.isActive())
                .build();
    }
}
