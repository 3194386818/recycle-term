package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class TaskUpdateDto {
    private Boolean needVisit;
    private Boolean completed;
    private Integer status;
    private String remark;
    private String failReason;
}
