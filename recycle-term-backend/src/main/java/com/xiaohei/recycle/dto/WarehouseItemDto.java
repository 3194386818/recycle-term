package com.xiaohei.recycle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class WarehouseItemDto {
    private Long id;
    private String productId;
    private String customerName;
    private String phone;
    private String splitter;
    private String address;
    private String snNumber;
    private String accessRoom;
    private Boolean outbound;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime outboundAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedAt;
    private List<WarehouseDeviceDto> devices = new ArrayList<>();
}
