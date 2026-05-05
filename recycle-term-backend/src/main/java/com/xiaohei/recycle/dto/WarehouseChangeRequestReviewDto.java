package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class WarehouseChangeRequestReviewDto {
    private boolean approved;
    private String rejectReason;
}
