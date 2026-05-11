package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class WorkOrderStatusDto {
    private String status;
    private String failureReason;
    private String remark;
}
