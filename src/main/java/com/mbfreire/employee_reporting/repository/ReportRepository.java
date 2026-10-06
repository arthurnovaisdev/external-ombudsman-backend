package com.mbfreire.employee_reporting.repository;

import com.mbfreire.employee_reporting.entity.Report;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository
        extends JpaRepository<Report, UUID> {

    boolean existsByProtocol(String protocol);


    @EntityGraph(attributePaths = {"category", "owner"})
    Optional<Report> findByProtocol(String protocol);


    @EntityGraph(attributePaths = {"category", "owner"})
    Optional<Report> findByProtocolAndOwnerId(
            String protocol,
            UUID ownerId
    );


    @EntityGraph(attributePaths = {"category", "owner"})
    Page<Report> findByOwnerId(
            UUID ownerId,
            Pageable pageable
    );


    @Override
    @EntityGraph(attributePaths = {"category", "owner"})
    Page<Report> findAll(Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from Report r
            join fetch r.owner
            join fetch r.category
            where r.protocol = :protocol
            """)
    Optional<Report> findByProtocolForUpdate(
            @Param("protocol") String protocol
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from Report r
            join fetch r.owner
            join fetch r.category
            where r.protocol = :protocol
              and r.owner.id = :ownerId
            """)
    Optional<Report> findByProtocolAndOwnerIdForUpdate(
            @Param("protocol") String protocol,
            @Param("ownerId") UUID ownerId
    );
}