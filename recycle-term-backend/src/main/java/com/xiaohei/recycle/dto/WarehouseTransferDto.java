package com.xiaohei.recycle.dto;

import lombok.Data;

import java.util.List;

@Data
public class WarehouseTransferDto {
    private Long toWarehouseId;
    private List<Long> deviceIds;
    private String remark;
}
