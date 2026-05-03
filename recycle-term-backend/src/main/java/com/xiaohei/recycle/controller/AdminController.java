package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.LoginRequest;
import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.dto.TaskCreateDto;
import com.xiaohei.recycle.entity.OperationLog;
import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.entity.Engineer;
import com.xiaohei.recycle.service.AdminService;
import com.xiaohei.recycle.service.EngineerService;
import com.xiaohei.recycle.service.RecycleTaskService;
import com.xiaohei.recycle.service.StatsService;
import com.xiaohei.recycle.service.EngineerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StatsService statsService;
    private final RecycleTaskService taskService;
    private final EngineerService engineerService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest req) {
        try {
            Map<String, Object> data = adminService.login(request);
            adminService.log(null, request.getUsername(), "登录", "管理员登录", getClientIp(req));
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/tasks")
    public Result<Page<RecycleTask>> getTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(adminService.getTasks(keyword, status, pageable));
    }

    @PostMapping("/tasks")
    public Result<RecycleTask> createTask(@RequestBody TaskCreateDto dto, HttpServletRequest req) {
        RecycleTask task = adminService.createTask(dto);
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        adminService.log(adminId, username, "添加任务", "添加任务: " + dto.getUserName() + " (" + dto.getPhoneNumber() + ")", getClientIp(req));
        return Result.ok("添加成功", task);
    }

    @PostMapping("/tasks/batch")
    public Result<List<RecycleTask>> batchCreateTasks(@RequestBody List<TaskCreateDto> dtos, HttpServletRequest req) {
        List<RecycleTask> tasks = adminService.batchCreateTasks(dtos);
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        adminService.log(adminId, username, "批量添加", "批量添加 " + tasks.size() + " 条任务", getClientIp(req));
        return Result.ok("批量添加成功，共 " + tasks.size() + " 条", tasks);
    }

    @PutMapping("/tasks/{id}")
    public Result<RecycleTask> updateTask(@PathVariable Long id, @RequestBody TaskCreateDto dto, HttpServletRequest req) {
        RecycleTask task = adminService.updateTask(id, dto);
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        adminService.log(adminId, username, "修改任务", "修改任务 ID=" + id + ": " + dto.getUserName(), getClientIp(req));
        return Result.ok("修改成功", task);
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id, HttpServletRequest req) {
        adminService.deleteTask(id);
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        adminService.log(adminId, username, "删除任务", "删除任务 ID=" + id, getClientIp(req));
        return Result.ok("删除成功", null);
    }

    @DeleteMapping("/tasks/batch")
    public Result<Void> batchDeleteTasks(@RequestBody List<Long> ids, HttpServletRequest req) {
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        adminService.batchDeleteTasks(ids);
        adminService.log(adminId, username, "批量删除", "批量删除 " + ids.size() + " 条任务: " + ids, getClientIp(req));
        return Result.ok("批量删除成功，共 " + ids.size() + " 条", null);
    }

    @GetMapping("/stats/daily")
    public Result<List<Map<String, Object>>> dailyStats(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.getDailyStats(days));
    }

    @GetMapping("/stats/status")
    public Result<Map<String, Long>> statusStats() {
        return Result.ok(statsService.getStatusStats());
    }

    @GetMapping("/stats/area")
    public Result<List<Map<String, Object>>> areaStats() {
        return Result.ok(statsService.getAreaStats());
    }

    @GetMapping("/logs")
    public Result<Page<OperationLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        return Result.ok(adminService.getLogs(pageable));
    }

    @PatchMapping("/tasks/{id}/review")
    public Result<RecycleTask> review(@PathVariable Long id, @RequestParam boolean approved,
                                      @RequestParam(required = false) String reviewRemark,
                                      HttpServletRequest req) {
        Long adminId = (Long) req.getAttribute("adminId");
        String username = (String) req.getAttribute("adminUsername");
        RecycleTask task = taskService.review(id, approved, reviewRemark, adminId);
        adminService.log(adminId, username, "审核任务",
                "审核任务 ID=" + id + " " + (approved ? "通过" : "驳回") + " " + (reviewRemark != null ? reviewRemark : ""),
                getClientIp(req));
        return Result.ok(approved ? "已归档" : "已驳回", task);
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

    @GetMapping("/engineers")
    public Result<Page<Engineer>> getEngineers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(engineerService.getEngineers(PageRequest.of(page, size)));
    }

    @PostMapping("/engineers")
    public Result<Engineer> createEngineer(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String name = body.get("name");
        if (phone == null || phone.isBlank()) {
            return Result.error("手机号不能为空");
        }
        try {
            return Result.ok("创建成功", engineerService.createEngineer(phone, name));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/engineers/{id}")
    public Result<Void> deleteEngineer(@PathVariable Long id) {
        engineerService.deleteEngineer(id);
        return Result.ok("删除成功", null);
    }

    @PostMapping("/engineers/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        engineerService.resetPassword(id);
        return Result.ok("密码已重置为 admin123", null);
    }
}
