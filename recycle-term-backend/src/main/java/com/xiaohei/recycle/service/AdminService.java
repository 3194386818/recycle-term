package com.xiaohei.recycle.service;

import com.xiaohei.recycle.dto.LoginRequest;
import com.xiaohei.recycle.dto.TaskCreateDto;
import com.xiaohei.recycle.entity.AdminUser;
import com.xiaohei.recycle.entity.OperationLog;
import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.repository.AdminUserRepository;
import com.xiaohei.recycle.repository.OperationLogRepository;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import com.xiaohei.recycle.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final OperationLogRepository logRepository;
    private final RecycleTaskRepository taskRepository;
    private final JwtUtil jwtUtil;

    @PostConstruct
    public void init() {
        if (!adminUserRepository.existsByUsername("admin")) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(hashPassword("admin123"));
            adminUserRepository.save(admin);
        }
    }

    public Map<String, Object> login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!user.getPassword().equals(hashPassword(request.getPassword()))) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        return result;
    }

    public Page<RecycleTask> getTasks(String keyword, Integer status, Pageable pageable) {
        Specification<RecycleTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("phoneNumber"), like),
                    cb.like(root.get("productId"), like),
                    cb.like(root.get("userName"), like),
                    cb.like(root.get("userAddress"), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return taskRepository.findAll(spec, pageable);
    }

    public RecycleTask createTask(TaskCreateDto dto) {
        RecycleTask task = new RecycleTask();
        copyDtoToEntity(dto, task);
        return taskRepository.save(task);
    }

    public List<RecycleTask> batchCreateTasks(List<TaskCreateDto> dtos) {
        List<RecycleTask> tasks = new ArrayList<>();
        for (TaskCreateDto dto : dtos) {
            RecycleTask task = new RecycleTask();
            copyDtoToEntity(dto, task);
            tasks.add(taskRepository.save(task));
        }
        return tasks;
    }

    public RecycleTask updateTask(Long id, TaskCreateDto dto) {
        RecycleTask task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        copyDtoToEntity(dto, task);
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("任务不存在");
        }
        taskRepository.deleteById(id);
    }

    public void log(Long adminId, String adminUsername, String action, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        logRepository.save(log);
    }

    public Page<OperationLog> getLogs(Pageable pageable) {
        return logRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private void copyDtoToEntity(TaskCreateDto dto, RecycleTask task) {
        task.setPhoneNumber(dto.getPhoneNumber());
        task.setProductId(dto.getProductId());
        task.setUserName(dto.getUserName());
        task.setUserAddress(dto.getUserAddress());
        task.setArea(dto.getArea());
        task.setEngineerName(dto.getEngineerName());
        task.setEngineerPhone(dto.getEngineerPhone());
        task.setDetailDesc(dto.getDetailDesc());
        task.setTerminals(dto.getTerminals());
        task.setExpectedCount(dto.getExpectedCount());
        task.setFttrCount(dto.getFttrCount());
        task.setAccessRoom(dto.getAccessRoom());
        task.setCategory(dto.getCategory());
        task.setDevDept(dto.getDevDept());
        task.setDevPerson(dto.getDevPerson());
        if (dto.getNeedVisit() != null) task.setNeedVisit(dto.getNeedVisit());
        task.setRemark(dto.getRemark());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(("recycle-salt:" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
