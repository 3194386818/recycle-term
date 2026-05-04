package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.entity.DeviceType;
import com.xiaohei.recycle.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeService service;

    @GetMapping
    public Result<List<DeviceType>> getAll(
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return Result.ok(service.getAll(sortBy, sortOrder));
    }

    @PostMapping
    public Result<DeviceType> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return Result.error("类型名称不能为空");
        }
        try {
            return Result.ok("添加成功", service.create(name));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok("删除成功", null);
    }
}
