package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class WarehouseChangeRequestCreateDto {
    private Long warehouseItemId;
    private String requestType;
    private String requestContent;
    private String requestedBy;
}
