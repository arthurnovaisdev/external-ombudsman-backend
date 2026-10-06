package com.mbfreire.employee_reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "report_id",
            nullable = false
    )
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_user_id",
            nullable = false
    )
    private User author;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String body;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}