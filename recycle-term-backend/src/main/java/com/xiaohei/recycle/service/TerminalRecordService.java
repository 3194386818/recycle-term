package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.entity.TerminalRecord;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import com.xiaohei.recycle.repository.TerminalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminalRecordService {

    private final TerminalRecordRepository recordRepository;
    private final RecycleTaskRepository taskRepository;

    public List<TerminalRecord> getByTaskId(Long taskId) {
        return recordRepository.findByTaskIdOrderByScannedAtDesc(taskId);
    }

    @Transactional
    public List<TerminalRecord> scan(Long taskId, List<String> serialNumbers) {
        RecycleTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        List<TerminalRecord> records = new ArrayList<>();
        for (String sn : serialNumbers) {
            if (sn == null || sn.isBlank()) continue;
            TerminalRecord record = new TerminalRecord();
            record.setTaskId(taskId);
            record.setSerialNumber(sn.trim());
            records.add(recordRepository.save(record));
        }

        // Check if all terminals scanned, auto-complete
        long scannedCount = recordRepository.findByTaskIdOrderByScannedAtDesc(taskId).size();
        if (task.getExpectedCount() != null && scannedCount >= task.getExpectedCount() && !task.getCompleted()) {
            task.setCompleted(true);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        }

        return records;
    }

    @Transactional
    public void deleteById(Long id) {
        recordRepository.deleteById(id);
    }
}
