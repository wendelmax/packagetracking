package com.packagetracking.command.repository;

import com.packagetracking.command.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageJpaRepository extends JpaRepository<Package, String> {
    
    /**
     * Verifica se existe pacote com ID
     */
    boolean existsById(String id);
    
    /**
     * Busca por ID retornando Optional
     */
    Optional<Package> findById(String id);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.status = :status ORDER BY p.updated_at DESC", nativeQuery = true)
    Page<Package> findByStatusWithPagination(@Param("status") String status, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.created_at < :cutoffDate AND p.status = :status", nativeQuery = true)
    List<Package> findOldPackagesForCleanup(@Param("cutoffDate") Instant cutoffDate, @Param("status") String status);
    
    @Modifying
    @Query(value = "DELETE FROM packages WHERE created_at < :cutoffDate AND status = :status", nativeQuery = true)
    int deleteOldPackages(@Param("cutoffDate") Instant cutoffDate, @Param("status") String status);
    
    @Query(value = "SELECT COUNT(*) FROM packages WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.sender = :sender ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findBySenderWithPagination(@Param("sender") String sender, Pageable pageable);
    
    @Query(value = "SELECT p.* FROM packages p WHERE p.recipient = :recipient ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Package> findByRecipientWithPagination(@Param("recipient") String recipient, Pageable pageable);
} 