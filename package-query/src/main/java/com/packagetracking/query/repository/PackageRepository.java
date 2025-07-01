package com.packagetracking.query.repository;

import com.packagetracking.query.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, String> {
    
    Optional<Package> findById(String id);
    
    List<Package> findBySender(String sender);
    List<Package> findByRecipient(String recipient);
    List<Package> findBySenderAndRecipient(String sender, String recipient);
    @Query(value = "SELECT p.* FROM packages p WHERE (:sender IS NULL OR p.sender = :sender) AND (:recipient IS NULL OR p.recipient = :recipient) ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findBySenderAndRecipient(@Param("sender") String sender, @Param("recipient") String recipient, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.sender = :sender ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findBySender(@Param("sender") String sender, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.recipient = :recipient ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findByRecipient(@Param("recipient") String recipient, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.status = :status ORDER BY p.updated_at DESC", nativeQuery = true)
    Page<Package> findByStatus(@Param("status") String status, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.status = :status AND p.created_at < :cutoffDate ORDER BY p.created_at ASC", nativeQuery = true)
    Page<Package> findOldPackagesByStatus(@Param("status") String status, @Param("cutoffDate") Instant cutoffDate, Pageable pageable);
    
    @Query(value = "SELECT COUNT(*) FROM packages WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.estimated_delivery_date BETWEEN :startDate AND :endDate ORDER BY p.estimated_delivery_date ASC", nativeQuery = true)
    Page<Package> findByEstimatedDeliveryDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.sender = :sender AND p.status = :status ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findBySenderAndStatus(@Param("sender") String sender, @Param("status") String status, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.recipient = :recipient AND p.status = :status ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findByRecipientAndStatus(@Param("recipient") String recipient, @Param("status") String status, Pageable pageable);
} 