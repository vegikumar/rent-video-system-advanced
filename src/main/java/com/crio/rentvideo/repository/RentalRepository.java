package com.crio.rentvideo.repository;

import com.crio.rentvideo.entity.Rental;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    long countByUserIdAndActiveTrue(Long userId);

    List<Rental> findByUserIdAndActiveTrue(Long userId);

    Optional<Rental> findByUserIdAndVideoIdAndActiveTrue(Long userId, Long videoId);

    boolean existsByVideoIdAndActiveTrue(Long videoId);
}
