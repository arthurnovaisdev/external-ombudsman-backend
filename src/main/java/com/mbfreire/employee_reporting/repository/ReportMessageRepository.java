package com.mbfreire.employee_reporting.repository;

import com.mbfreire.employee_reporting.entity.ReportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ReportMessageRepository
        extends JpaRepository<ReportMessage, UUID> {

    @EntityGraph(
            attributePaths = "author"
    )
    Page<ReportMessage>
    findByReportIdOrderByCreatedAtAscIdAsc(
            UUID reportId,
            Pageable pageable
    );

    void deleteByReportId(
            UUID reportId
    );

    @Modifying
    @Query("""
            delete from ReportMessage m
            where m.report.id in :reportIds
            """)
    int deleteByReportIdIn(
            @Param("reportIds")
            Collection<UUID> reportIds
    );
}