package com.xiaohei.recycle.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderDto {
    private String workOrderNo;
    private String productId;
    private String userName;
    private String contactPhone;
    private String workOrderType;
    private String address;
    private String cvlan;
    private String svlan;
    private String splitter;
    private String splitterPort;
    private String onuSn;
    private String status;
    private String failureReason;
    private String remark;
    private LocalDateTime appointedAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime completedAt;
    private LocalDateTime transferredAt;
    private LocalDateTime failedAt;
    private String sourceType;
    private String sourceId;
    private String rawSource;
}
