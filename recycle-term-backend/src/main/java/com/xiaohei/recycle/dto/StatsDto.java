package com.xiaohei.recycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsDto {
    private long total;
    private long completed;
    private long pending;
    private long needVisit;
    private long totalScanned;
}
