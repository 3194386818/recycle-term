package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.TerminalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TerminalRecordRepository extends JpaRepository<TerminalRecord, Long> {
    List<TerminalRecord> findByTaskIdOrderByScannedAtDesc(Long taskId);
    void deleteByTaskId(Long taskId);
    void deleteByTaskIdIn(List<Long> taskIds);
    long countByTaskId(Long taskId);
    long countByScannedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', r.scannedAt, '%Y-%m-%d'), COUNT(r) FROM TerminalRecord r WHERE r.scannedAt BETWEEN :start AND :end GROUP BY FUNCTION('DATE_FORMAT', r.scannedAt, '%Y-%m-%d')")
    List<Object[]> countScannedGroupByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
