package com.crio.rentvideo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crio.rentvideo.dto.RentalResponse;
import com.crio.rentvideo.entity.Rental;
import com.crio.rentvideo.entity.Role;
import com.crio.rentvideo.entity.User;
import com.crio.rentvideo.entity.Video;
import com.crio.rentvideo.exception.BadRequestException;
import com.crio.rentvideo.repository.RentalRepository;
import com.crio.rentvideo.repository.UserRepository;
import com.crio.rentvideo.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoService videoService;

    @InjectMocks
    private RentalService rentalService;

    private User customer;
    private Video video;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .password("encoded")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .build();

        video = Video.builder()
                .id(10L)
                .title("Inception")
                .director("Christopher Nolan")
                .genre("Sci-Fi")
                .available(true)
                .build();

        CustomUserDetails details = new CustomUserDetails(customer);
        authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
    }

    @Test
    void rentVideo_success() {
        when(videoService.findVideo(10L)).thenReturn(video);
        when(rentalRepository.findByUserIdAndVideoIdAndActiveTrue(1L, 10L)).thenReturn(Optional.empty());
        when(rentalRepository.countByUserIdAndActiveTrue(1L)).thenReturn(0L);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental rental = invocation.getArgument(0);
            rental.setId(100L);
            return rental;
        });

        RentalResponse response = rentalService.rentVideo(10L, authentication);

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getVideoId());
        assertTrue(response.isActive());
        assertEquals(false, video.isAvailable());
    }

    @Test
    void rentVideo_rejectsWhenAlreadyHasTwoActiveRentals() {
        when(videoService.findVideo(10L)).thenReturn(video);
        when(rentalRepository.findByUserIdAndVideoIdAndActiveTrue(1L, 10L)).thenReturn(Optional.empty());
        when(rentalRepository.countByUserIdAndActiveTrue(1L)).thenReturn(2L);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> rentalService.rentVideo(10L, authentication));

        assertTrue(ex.getMessage().contains("Maximum of 2"));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void rentVideo_rejectsUnavailableVideo() {
        video.setAvailable(false);
        when(videoService.findVideo(10L)).thenReturn(video);

        assertThrows(BadRequestException.class, () -> rentalService.rentVideo(10L, authentication));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void returnVideo_success() {
        Rental activeRental = Rental.builder()
                .id(50L)
                .user(customer)
                .video(video)
                .rentedAt(LocalDateTime.now().minusDays(1))
                .active(true)
                .build();
        video.setAvailable(false);

        when(videoService.findVideo(10L)).thenReturn(video);
        when(rentalRepository.findByUserIdAndVideoIdAndActiveTrue(1L, 10L))
                .thenReturn(Optional.of(activeRental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentalResponse response = rentalService.returnVideo(10L, authentication);

        assertEquals(false, response.isActive());
        assertTrue(video.isAvailable());
        assertEquals(50L, response.getId());
    }
}
