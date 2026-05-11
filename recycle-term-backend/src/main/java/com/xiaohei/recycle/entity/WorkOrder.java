package com.xiaohei.recycle.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "work_order", indexes = {
        @Index(name = "idx_work_order_no", columnList = "work_order_no", unique = true),
        @Index(name = "idx_work_order_status", columnList = "status"),
        @Index(name = "idx_work_order_type", columnList = "work_order_type"),
        @Index(name = "idx_work_order_source", columnList = "source_type, source_id", unique = true)
})
public class WorkOrder {

    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_APPOINTED = "APPOINTED";
    public static final String STATUS_FULFILLING = "FULFILLING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_TRANSFERRED = "TRANSFERRED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_no", length = 20, nullable = false, unique = true)
    private String workOrderNo;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "user_name", length = 50)
    private String userName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "work_order_type", length = 50)
    private String workOrderType;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String cvlan;

    @Column(length = 50)
    private String svlan;

    @Column(length = 100)
    private String splitter;

    @Column(name = "splitter_port", length = 50)
    private String splitterPort;

    @Column(name = "onu_sn", length = 100)
    private String onuSn;

    @Column(length = 30, nullable = false)
    private String status = STATUS_ACCEPTED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(length = 500)
    private String remark;

    @Column(name = "appointed_at")
    private LocalDateTime appointedAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "raw_source", columnDefinition = "TEXT")
    private String rawSource;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
