package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouses")
@RequiredArgsConstructor
public class AdminWarehouseController {
    private final WarehouseService warehouseService;

    @GetMapping
    public Result<List<WarehouseDto>> list(@RequestParam(defaultValue = "true") boolean includeDisabled) {
        return Result.ok(warehouseService.listWarehouses(includeDisabled));
    }

    @PostMapping
    public Result<WarehouseDto> create(@RequestBody WarehouseCreateDto dto) {
        return Result.ok("仓库已创建", warehouseService.createWarehouse(dto));
    }

    @PutMapping("/{id}")
    public Result<WarehouseDto> update(@PathVariable Long id, @RequestBody WarehouseCreateDto dto) {
        return Result.ok("仓库已更新", warehouseService.updateWarehouse(id, dto));
    }

    @PatchMapping("/{id}/enabled")
    public Result<WarehouseDto> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.ok(enabled ? "仓库已启用" : "仓库已禁用", warehouseService.setWarehouseEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.ok("仓库已删除", null);
    }

    @PostMapping("/devices/batch-transfer")
    public Result<List<WarehouseDeviceDto>> batchTransfer(@RequestBody WarehouseTransferDto dto, HttpServletRequest req) {
        return Result.ok("批量转仓成功", warehouseService.transferDevices(dto.getDeviceIds(), dto.getToWarehouseId(), dto.getRemark()));
    }
}
