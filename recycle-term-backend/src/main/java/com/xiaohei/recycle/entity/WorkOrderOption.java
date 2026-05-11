package com.xiaohei.recycle.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "work_order_option", indexes = {
        @Index(name = "idx_work_order_option_category", columnList = "category"),
        @Index(name = "idx_work_order_option_unique", columnList = "category, value", unique = true)
})
public class WorkOrderOption {

    public static final String CATEGORY_TYPE = "TYPE";
    public static final String CATEGORY_FAILURE_REASON = "FAILURE_REASON";
    public static final String CATEGORY_STATUS = "STATUS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(length = 100, nullable = false)
    private String value;

    @Column(length = 100, nullable = false)
    private String label;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
