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

@Service
@RequiredArgsConstructor
public class RecycleTaskService {

    private final RecycleTaskRepository taskRepository;
    private final TerminalRecordRepository recordRepository;

    public Page<RecycleTask> search(String keyword, Boolean completed, Boolean needVisit, String status, Pageable pageable) {
        Specification<RecycleTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("phoneNumber"), like),
                    cb.like(root.get("userName"), like),
                    cb.like(root.get("userAddress"), like),
                    cb.like(root.get("terminals"), like),
                    cb.like(root.get("detailDesc"), like)
                ));
            }
            if (StringUtils.hasText(status)) {
                if ("待回收".equals(status)) {
                    predicates.add(cb.or(
                        cb.equal(root.get("status"), status),
                        cb.isNull(root.get("status"))
                    ));
                } else {
                    predicates.add(cb.equal(root.get("status"), status));
                }
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
        if (dto.getRemark() != null) {
            task.setRemark(dto.getRemark());
        }
        return taskRepository.save(task);
    }

    public StatsDto getStats() {
        long total = taskRepository.count();
        long completed = taskRepository.count((root, query, cb) -> cb.equal(root.get("status"), "已完成"));
        long pending = taskRepository.count((root, query, cb) -> cb.or(
            cb.equal(root.get("status"), "待回收"),
            cb.isNull(root.get("status"))
        ));
        long needVisit = taskRepository.count((root, query, cb) -> cb.equal(root.get("needVisit"), true));
        long failed = taskRepository.count((root, query, cb) -> cb.equal(root.get("status"), "已失败"));
        long totalScanned = recordRepository.count();
        return new StatsDto(total, completed, pending, needVisit, failed, totalScanned);
    }

    @Transactional
    public void deleteById(Long id) {
        recordRepository.deleteByTaskId(id);
        taskRepository.deleteById(id);
    }
}
