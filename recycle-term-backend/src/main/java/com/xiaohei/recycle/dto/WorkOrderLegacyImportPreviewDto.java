package com.xiaohei.recycle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderLegacyImportPreviewDto {
    private long total;
    private long importable;
    private long existing;
    private List<WorkOrderDto> samples;
}
