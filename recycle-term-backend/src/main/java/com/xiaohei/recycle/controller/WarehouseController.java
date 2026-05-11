package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.entity.WarehouseChangeRequest;
import com.xiaohei.recycle.service.WarehouseChangeRequestService;
import com.xiaohei.recycle.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseChangeRequestService requestService;

    @GetMapping
    public Result<Page<WarehouseItemDto>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "false") boolean outboundOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        return Result.ok(warehouseService.search(keyword, warehouseId, includeDisabled, outboundOnly, pageable));
    }

    @GetMapping("/warehouses")
    public Result<List<WarehouseDto>> listWarehouses(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        return Result.ok(warehouseService.listWarehouses(includeDisabled));
    }

    @GetMapping("/{id}")
    public Result<WarehouseItemDto> getById(@PathVariable Long id,
                                            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return Result.ok(warehouseService.getDtoById(id, includeDisabled));
    }

    @PostMapping
    public Result<WarehouseItemDto> create(@RequestBody WarehouseItemDto item) {
        return Result.ok("入库成功", warehouseService.create(item));
    }

    @PutMapping("/{id}")
    public Result<WarehouseItemDto> update(@PathVariable Long id, @RequestBody WarehouseItemDto item) {
        return Result.ok("修改成功", warehouseService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok("删除成功", null);
    }

    @PostMapping("/{id}/outbound")
    public Result<WarehouseItemDto> outbound(@PathVariable Long id, @RequestParam String sn) {
        return Result.ok("出库成功", warehouseService.outboundDevice(id, sn));
    }

    @PostMapping("/devices/{deviceId}/outbound")
    public Result<WarehouseDeviceDto> outboundDevice(@PathVariable Long deviceId) {
        return Result.ok("出库成功", warehouseService.outboundDeviceById(deviceId));
    }

    @PostMapping("/devices/{deviceId}/transfer")
    public Result<WarehouseDeviceDto> transferDevice(@PathVariable Long deviceId, @RequestBody WarehouseTransferDto dto) {
        return Result.ok("转仓成功", warehouseService.transferDevice(deviceId, dto.getToWarehouseId(), dto.getRemark()));
    }

    @GetMapping("/devices/{deviceId}/movements")
    public Result<List<WarehouseDeviceMovementDto>> listMovements(@PathVariable Long deviceId) {
        return Result.ok(warehouseService.listMovements(deviceId));
    }

    @PostMapping("/requests")
    public Result<WarehouseChangeRequest> createRequest(@RequestBody WarehouseChangeRequestCreateDto dto) {
        return Result.ok("申请已提交", requestService.create(dto));
    }
}
