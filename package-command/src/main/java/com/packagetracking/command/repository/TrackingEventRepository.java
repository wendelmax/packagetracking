package com.packagetracking.command.repository;

import com.packagetracking.command.entity.TrackingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, String> {
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId ORDER BY te.date_time DESC", nativeQuery = true)
    Page<TrackingEvent> findByPackageIdWithPagination(@Param("packageId") String packageId, Pageable pageable);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.date_time < :cutoffDate", nativeQuery = true)
    List<TrackingEvent> findOldEventsForCleanup(@Param("cutoffDate") Instant cutoffDate);
    
    @Modifying
    @Query(value = "DELETE FROM tracking_events WHERE date_time < :cutoffDate", nativeQuery = true)
    int deleteOldEvents(@Param("cutoffDate") Instant cutoffDate);
    
    @Query(value = "SELECT COUNT(*) FROM tracking_events WHERE package_id = :packageId", nativeQuery = true)
    long countByPackageId(@Param("packageId") String packageId);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.date_time BETWEEN :startDate AND :endDate ORDER BY te.date_time DESC", nativeQuery = true)
    Page<TrackingEvent> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, Pageable pageable);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId AND te.date_time >= :sinceDate ORDER BY te.date_time ASC", nativeQuery = true)
    List<TrackingEvent> findRecentEventsByPackageId(@Param("packageId") String packageId, @Param("sinceDate") Instant sinceDate);
} 