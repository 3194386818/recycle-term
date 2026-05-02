package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.dto.StatsDto;
import com.xiaohei.recycle.dto.TaskUpdateDto;
import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.service.RecycleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final RecycleTaskService taskService;

    @GetMapping
    public Result<Page<RecycleTask>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean needVisit,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Page<RecycleTask> result = taskService.search(
                keyword, completed, needVisit, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<RecycleTask> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<RecycleTask> update(@PathVariable Long id, @RequestBody TaskUpdateDto dto) {
        return Result.ok(taskService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<RecycleTask> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(taskService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @GetMapping("/stats")
    public Result<StatsDto> stats() {
        return Result.ok(taskService.getStats());
    }
}
