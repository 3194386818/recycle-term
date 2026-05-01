package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.TerminalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalRecordRepository extends JpaRepository<TerminalRecord, Long> {
    List<TerminalRecord> findByTaskIdOrderByScannedAtDesc(Long taskId);
    void deleteByTaskId(Long taskId);
}
