package com.xiaohei.recycle.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recycle_task")
public class RecycleTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "user_name", length = 50)
    private String userName;

    @Column(name = "user_address", length = 500)
    private String userAddress;

    @Column(length = 50)
    private String area;

    @Column(name = "engineer_name", length = 50)
    private String engineerName;

    @Column(name = "engineer_phone", length = 20)
    private String engineerPhone;

    @Column(name = "detail_desc", columnDefinition = "TEXT")
    private String detailDesc;

    @Column(columnDefinition = "TEXT")
    private String terminals;

    @Column(name = "expected_count")
    private Integer expectedCount;

    @Column(name = "fttr_count")
    private Integer fttrCount;

    @Column(name = "access_room", length = 100)
    private String accessRoom;

    @Column(length = 50)
    private String category;

    @Column(name = "dev_dept", length = 100)
    private String devDept;

    @Column(name = "dev_person", length = 50)
    private String devPerson;

    @Column(name = "need_visit")
    private Boolean needVisit = false;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
