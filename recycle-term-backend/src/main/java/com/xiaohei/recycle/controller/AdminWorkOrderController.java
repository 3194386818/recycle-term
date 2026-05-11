package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.entity.WorkOrder;
import com.xiaohei.recycle.entity.WorkOrderOption;
import com.xiaohei.recycle.service.AdminService;
import com.xiaohei.recycle.service.WorkOrderOptionService;
import com.xiaohei.recycle.service.WorkOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/work-orders")
@RequiredArgsConstructor
public class AdminWorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderOptionService optionService;
    private final AdminService adminService;

    @GetMapping
    public Result<Page<WorkOrder>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(workOrderService.search(keyword, status, type, pageable));
    }

    @GetMapping("/{id}")
    public Result<WorkOrder> getById(@PathVariable Long id) {
        return Result.ok(workOrderService.getById(id));
    }

    @PostMapping
    public Result<WorkOrder> create(@RequestBody WorkOrderDto dto, HttpServletRequest req) {
        WorkOrder workOrder = workOrderService.create(dto);
        log(req, "添加工单", "添加工单: " + workOrder.getWorkOrderNo() + describeUser(workOrder));
        return Result.ok("创建成功", workOrder);
    }

    @PutMapping("/{id}")
    public Result<WorkOrder> update(@PathVariable Long id, @RequestBody WorkOrderDto dto, HttpServletRequest req) {
        WorkOrder workOrder = workOrderService.update(id, dto);
        log(req, "修改工单", "修改工单 ID=" + id + ": " + workOrder.getWorkOrderNo() + describeUser(workOrder));
        return Result.ok("修改成功", workOrder);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        WorkOrder workOrder = workOrderService.getById(id);
        workOrderService.delete(id);
        log(req, "删除工单", "删除工单 ID=" + id + ": " + workOrder.getWorkOrderNo());
        return Result.ok("删除成功", null);
    }

    @PatchMapping("/{id}/status")
    public Result<WorkOrder> updateStatus(@PathVariable Long id, @RequestBody WorkOrderStatusDto dto, HttpServletRequest req) {
        WorkOrder workOrder = workOrderService.updateStatus(id, dto);
        log(req, "更新工单状态", "更新工单 ID=" + id + " 状态为 " + workOrder.getStatus() + ": " + workOrder.getWorkOrderNo());
        return Result.ok("状态已更新", workOrder);
    }

    @GetMapping("/legacy-import/preview")
    public Result<WorkOrderLegacyImportPreviewDto> previewLegacyImport(@RequestParam(defaultValue = "20") int sampleSize) {
        return Result.ok(workOrderService.previewLegacyImport(sampleSize));
    }

    @PostMapping("/legacy-import")
    public Result<WorkOrderLegacyImportResultDto> importLegacy(HttpServletRequest req) {
        WorkOrderLegacyImportResultDto result = workOrderService.importLegacy();
        log(req, "导入旧仓库工单", "旧仓库工单导入完成，新增 " + result.getCreated() + " 条，跳过 " + result.getSkipped() + " 条");
        return Result.ok("导入完成", result);
    }

    @GetMapping("/options")
    public Result<WorkOrderOptionsDto> options() {
        return Result.ok(optionService.getOptions());
    }

    @GetMapping("/option-items")
    public Result<List<WorkOrderOption>> listOptionItems(@RequestParam(required = false) String category) {
        return Result.ok(optionService.list(category));
    }

    @PostMapping("/option-items")
    public Result<WorkOrderOption> createOption(@RequestBody WorkOrderOptionDto dto, HttpServletRequest req) {
        WorkOrderOption option = optionService.create(dto);
        log(req, "添加工单选项", "添加工单选项: " + option.getCategory() + " / " + option.getLabel());
        return Result.ok("添加成功", option);
    }

    @PutMapping("/option-items/{id}")
    public Result<WorkOrderOption> updateOption(@PathVariable Long id, @RequestBody WorkOrderOptionDto dto, HttpServletRequest req) {
        WorkOrderOption option = optionService.update(id, dto);
        log(req, "修改工单选项", "修改工单选项 ID=" + id + ": " + option.getCategory() + " / " + option.getLabel());
        return Result.ok("修改成功", option);
    }

    @DeleteMapping("/option-items/{id}")
    public Result<Void> deleteOption(@PathVariable Long id, HttpServletRequest req) {
        optionService.delete(id);
        log(req, "删除工单选项", "删除工单选项 ID=" + id);
        return Result.ok("删除成功", null);
    }

    private void log(HttpServletRequest request, String action, String detail) {
        Long adminId = (Long) request.getAttribute("adminId");
        String username = (String) request.getAttribute("adminUsername");
        adminService.log(adminId, username, action, detail, getClientIp(request));
    }

    private String describeUser(WorkOrder workOrder) {
        String userName = workOrder.getUserName() == null ? "" : " " + workOrder.getUserName();
        String phone = workOrder.getContactPhone() == null ? "" : " (" + workOrder.getContactPhone() + ")";
        return userName + phone;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
