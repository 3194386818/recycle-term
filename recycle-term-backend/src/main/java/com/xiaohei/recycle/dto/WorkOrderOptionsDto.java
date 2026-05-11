package com.xiaohei.recycle.dto;

import com.xiaohei.recycle.entity.WorkOrderOption;
import lombok.Data;

import java.util.List;

@Data
public class WorkOrderOptionsDto {
    private List<WorkOrderOption> types;
    private List<WorkOrderOption> failureReasons;
    private List<WorkOrderOption> statuses;
}
