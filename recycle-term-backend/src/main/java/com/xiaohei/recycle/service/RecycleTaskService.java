package com.xiaohei.recycle.service;

import com.xiaohei.recycle.dto.StatsDto;
import com.xiaohei.recycle.dto.TaskUpdateDto;
import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import com.xiaohei.recycle.repository.TerminalRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecycleTaskService {

    private final RecycleTaskRepository taskRepository;
    private final TerminalRecordRepository recordRepository;

    public Page<RecycleTask> search(String keyword, Boolean completed, Boolean needVisit, Integer status, Pageable pageable) {
        Specification<RecycleTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("phoneNumber"), like),
                    cb.like(root.get("productId"), like),
                    cb.like(root.get("userName"), like),
                    cb.like(root.get("userAddress"), like),
                    cb.like(root.get("terminals"), like),
                    cb.like(root.get("detailDesc"), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                if (completed != null) {
                    predicates.add(cb.equal(root.get("completed"), completed));
                }
                if (needVisit != null) {
                    predicates.add(cb.equal(root.get("needVisit"), needVisit));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return taskRepository.findAll(spec, pageable);
    }

    public RecycleTask getById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    @Transactional
    public RecycleTask update(Long id, TaskUpdateDto dto) {
        RecycleTask task = getById(id);
        if (dto.getNeedVisit() != null) {
            task.setNeedVisit(dto.getNeedVisit());
        }
        if (dto.getCompleted() != null) {
            task.setCompleted(dto.getCompleted());
            task.setCompletedAt(dto.getCompleted() ? LocalDateTime.now() : null);
        }
        if (dto.getStatus() != null) {
            validateTransition(task.getStatus(), dto.getStatus());
            task.setStatus(dto.getStatus());
            if (dto.getStatus() == 2) {
                task.setCompleted(true);
                task.setCompletedAt(LocalDateTime.now());
            }
            if (dto.getStatus() == 0) {
                task.setCompleted(false);
                task.setCompletedAt(null);
                task.setFailReason(null);
                task.setReviewRemark(null);
            }
        }
        if (dto.getFailReason() != null) {
            task.setFailReason(dto.getFailReason());
        }
        if (dto.getRemark() != null) {
            task.setRemark(dto.getRemark());
        }
        return taskRepository.save(task);
    }

    @Transactional
    public RecycleTask updateStatus(Long id, Integer status, String failReason) {
        RecycleTask task = getById(id);
        validateTransition(task.getStatus(), status);
        task.setStatus(status);
        if (status == 1) {
            task.setNeedVisit(true);
        }
        if (status == 2) {
            task.setCompleted(true);
            task.setCompletedAt(LocalDateTime.now());
        }
        if (status == 3) {
            task.setFailReason(failReason);
        }
        if (status == 0) {
            task.setCompleted(false);
            task.setCompletedAt(null);
            task.setFailReason(null);
            task.setReviewRemark(null);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public RecycleTask review(Long id, boolean approved, String reviewRemark, Long reviewerId) {
        RecycleTask task = getById(id);
        if (task.getStatus() != 2 && task.getStatus() != 3) {
            throw new RuntimeException("当前状态不允许审核，只有已完成/已失败状态可以审核");
        }
        if (approved) {
            task.setStatus(4);
            task.setCompleted(true);
        } else {
            task.setStatus(5);
            task.setCompleted(false);
            task.setCompletedAt(null);
        }
        task.setReviewRemark(reviewRemark);
        task.setReviewerId(reviewerId);
        return taskRepository.save(task);
    }

    private void validateTransition(Integer from, Integer to) {
        boolean valid = switch (from) {
            case 0 -> to == 1 || to == 3;
            case 1 -> to == 2 || to == 3;
            case 2 -> to == 4 || to == 5;
            case 3 -> to == 4 || to == 5;
            case 5 -> to == 0;
            default -> false;
        };
        if (!valid) {
            throw new RuntimeException("不允许从" + statusLabel(from) + "切换到" + statusLabel(to));
        }
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 0 -> "待回收";
            case 1 -> "已上门";
            case 2 -> "已完成";
            case 3 -> "已失败";
            case 4 -> "审核成功";
            case 5 -> "审核失败";
            default -> "未知";
        };
    }

    public StatsDto getStats() {
        long total = taskRepository.count();
        long completed = taskRepository.count((root, query, cb) -> cb.equal(root.get("status"), 4));
        long pending = taskRepository.count((root, query, cb) -> cb.equal(root.get("status"), 0));
        long needVisit = taskRepository.count((root, query, cb) -> cb.equal(root.get("needVisit"), true));
        long failed = taskRepository.count((root, query, cb) -> cb.equal(root.get("status"), 3));
        long totalScanned = recordRepository.count();
        return new StatsDto(total, completed, pending, needVisit, failed, totalScanned);
    }

    @Transactional
    public void deleteById(Long id) {
        recordRepository.deleteByTaskId(id);
        taskRepository.deleteById(id);
    }

    @Transactional
    public int backfillProductId(Map<String, String> mapping) {
        int updated = 0;
        for (var entry : mapping.entrySet()) {
            List<RecycleTask> tasks = taskRepository.findByPhoneNumber(entry.getKey());
            for (RecycleTask task : tasks) {
                if (task.getProductId() == null || task.getProductId().isBlank()) {
                    task.setProductId(entry.getValue());
                    taskRepository.save(task);
                    updated++;
                }
            }
        }
        return updated;
    }
}
