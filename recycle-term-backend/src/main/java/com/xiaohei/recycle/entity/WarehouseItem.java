package com.xiaohei.recycle.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warehouse_item")
public class WarehouseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", length = 20, nullable = false)
    private String productId;

    @Column(name = "devices", columnDefinition = "TEXT")
    private String devices;

    @Column(name = "customer_name", length = 50)
    private String customerName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "splitter", length = 100)
    private String splitter;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "sn_number", length = 50)
    private String snNumber;

    @Column(name = "access_room", length = 100)
    private String accessRoom;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private LocalDateTime receivedAt;
}
