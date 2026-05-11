package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.entity.WorkOrder;
import com.xiaohei.recycle.service.WorkOrderOptionService;
import com.xiaohei.recycle.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderOptionService optionService;

    @GetMapping
    public Result<Page<WorkOrder>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(workOrderService.search(keyword, status, type, pageable));
    }

    @GetMapping("/{id}")
    public Result<WorkOrder> getById(@PathVariable Long id) {
        return Result.ok(workOrderService.getById(id));
    }

    @PostMapping
    public Result<WorkOrder> create(@RequestBody WorkOrderDto dto) {
        return Result.ok("创建成功", workOrderService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<WorkOrder> update(@PathVariable Long id, @RequestBody WorkOrderDto dto) {
        return Result.ok("修改成功", workOrderService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<WorkOrder> updateStatus(@PathVariable Long id, @RequestBody WorkOrderStatusDto dto) {
        return Result.ok("状态已更新", workOrderService.updateStatus(id, dto));
    }

    @PostMapping("/quick-parse")
    public Result<WorkOrderDto> quickParse(@RequestBody WorkOrderParseRequest request) {
        return Result.ok(workOrderService.quickParse(request == null ? null : request.getText()));
    }

    @PostMapping("/quick")
    public Result<WorkOrder> quickCreate(@RequestBody WorkOrderParseRequest request) {
        return Result.ok("创建成功", workOrderService.quickCreate(request == null ? null : request.getText()));
    }

    @GetMapping("/options")
    public Result<WorkOrderOptionsDto> options() {
        return Result.ok(optionService.getOptions());
    }
}
