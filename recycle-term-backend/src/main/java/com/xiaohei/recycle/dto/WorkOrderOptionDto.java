package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class WorkOrderOptionDto {
    private String category;
    private String value;
    private String label;
    private Integer sortOrder;
    private Boolean enabled;
}
