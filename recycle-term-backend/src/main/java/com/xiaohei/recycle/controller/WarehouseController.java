package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.entity.WarehouseItem;
import com.xiaohei.recycle.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public Result<Page<WarehouseItem>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(warehouseService.search(keyword, pageable));
    }

    @GetMapping("/{id}")
    public Result<WarehouseItem> getById(@PathVariable Long id) {
        return Result.ok(warehouseService.getById(id));
    }

    @PostMapping
    public Result<WarehouseItem> create(@RequestBody WarehouseItem item) {
        return Result.ok("入库成功", warehouseService.create(item));
    }

    @PutMapping("/{id}")
    public Result<WarehouseItem> update(@PathVariable Long id, @RequestBody WarehouseItem item) {
        return Result.ok("修改成功", warehouseService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok("删除成功", null);
    }
}
