package com.smartmobility.tripservice.repository;

import com.smartmobility.tripservice.entity.Trip;
import com.smartmobility.tripservice.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Trip> findByPassIdOrderByCreatedAtDesc(UUID passId);

    List<Trip> findByStatus(TripStatus status);

    List<Trip> findByUserIdAndStatus(UUID userId, TripStatus status);

    long countByUserIdAndStatus(UUID userId, TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.passId = :passId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.createdAt >= :startOfDay")
    List<Trip> findTodayCompletedTripsByPassId(
            @Param("passId") UUID passId,
            @Param("startOfDay") LocalDateTime startOfDay
    );
}
