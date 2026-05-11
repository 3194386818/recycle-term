package com.xiaohei.recycle.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouse_device_movement", indexes = {
        @Index(name = "idx_warehouse_movement_device", columnList = "device_id"),
        @Index(name = "idx_warehouse_movement_created", columnList = "created_at")
})
public class WarehouseDeviceMovement {
    public static final String TYPE_INBOUND = "INBOUND";
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_OUTBOUND = "OUTBOUND";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "from_warehouse_id")
    private Long fromWarehouseId;

    @Column(name = "to_warehouse_id")
    private Long toWarehouseId;

    @Column(name = "movement_type", length = 20, nullable = false)
    private String movementType;

    @Column(name = "operator_name", length = 80)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
