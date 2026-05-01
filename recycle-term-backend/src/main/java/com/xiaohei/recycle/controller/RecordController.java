package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.dto.ScanDto;
import com.xiaohei.recycle.entity.TerminalRecord;
import com.xiaohei.recycle.service.TerminalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final TerminalRecordService recordService;

    @GetMapping("/task/{taskId}")
    public Result<List<TerminalRecord>> getByTaskId(@PathVariable Long taskId) {
        return Result.ok(recordService.getByTaskId(taskId));
    }

    @PostMapping("/scan/{taskId}")
    public Result<List<TerminalRecord>> scan(@PathVariable Long taskId, @RequestBody ScanDto dto) {
        List<TerminalRecord> records = recordService.scan(taskId, dto.getSerialNumbers());
        return Result.ok("扫描成功，共 " + records.size() + " 条", records);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.deleteById(id);
        return Result.ok("删除成功", null);
    }
}
