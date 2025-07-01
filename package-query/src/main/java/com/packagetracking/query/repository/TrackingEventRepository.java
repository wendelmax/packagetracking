package com.packagetracking.query.repository;

import com.packagetracking.query.entity.TrackingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId ORDER BY te.date DESC", nativeQuery = true)
    List<TrackingEvent> findByPackageIdOrderByDateTimeDesc(@Param("packageId") String packageId);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId ORDER BY te.date ASC", nativeQuery = true)
    List<TrackingEvent> findByPackageIdOrderByDateTimeAsc(@Param("packageId") String packageId);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId ORDER BY te.date DESC", nativeQuery = true)
    Page<TrackingEvent> findByPackageIdWithPagination(@Param("packageId") String packageId, Pageable pageable);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.date BETWEEN :startDate AND :endDate ORDER BY te.date DESC", nativeQuery = true)
    Page<TrackingEvent> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, Pageable pageable);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.package_id = :packageId AND te.date >= :sinceDate ORDER BY te.date ASC", nativeQuery = true)
    List<TrackingEvent> findRecentEventsByPackageId(@Param("packageId") String packageId, @Param("sinceDate") Instant sinceDate);
    
    @Query(value = "SELECT COUNT(*) FROM tracking_events WHERE package_id = :packageId", nativeQuery = true)
    long countByPackageId(@Param("packageId") String packageId);
    
    @Query(value = "SELECT te.* FROM tracking_events te WHERE te.date < :cutoffDate ORDER BY te.date ASC", nativeQuery = true)
    Page<TrackingEvent> findOldEventsForCleanup(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);
} 