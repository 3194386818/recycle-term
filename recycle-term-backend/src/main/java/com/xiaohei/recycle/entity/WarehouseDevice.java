package com.xiaohei.recycle.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouse_device", indexes = {
        @Index(name = "idx_warehouse_device_item", columnList = "warehouse_item_id"),
        @Index(name = "idx_warehouse_device_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_warehouse_device_serial", columnList = "serial_number")
})
public class WarehouseDevice {
    public static final String STATUS_IN_STOCK = "IN_STOCK";
    public static final String STATUS_OUTBOUND = "OUTBOUND";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_item_id", nullable = false)
    private Long warehouseItemId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "device_type", length = 80)
    private String type;

    @Column(name = "serial_number", length = 120, nullable = false)
    private String sn;

    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_IN_STOCK;

    @Column(name = "outbound", nullable = false)
    private Boolean outbound = false;

    @Column(name = "inbound_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inboundAt;

    @Column(name = "outbound_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime outboundAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
