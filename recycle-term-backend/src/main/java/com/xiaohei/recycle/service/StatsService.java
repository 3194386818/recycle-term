package com.xiaohei.recycle.service;

import com.xiaohei.recycle.repository.RecycleTaskRepository;
import com.xiaohei.recycle.repository.TerminalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final RecycleTaskRepository taskRepository;
    private final TerminalRecordRepository recordRepository;

    public List<Map<String, Object>> getDailyStats(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        Map<String, Long> completedByDate = new HashMap<>();
        for (Object[] row : taskRepository.countCompletedGroupByDate(start, end)) {
            completedByDate.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<String, Long> scannedByDate = new HashMap<>();
        for (Object[] row : recordRepository.countScannedGroupByDate(start, end)) {
            scannedByDate.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.toString();
            long completed = completedByDate.getOrDefault(dateKey, 0L);
            long scanned = scannedByDate.getOrDefault(dateKey, 0L);

            Map<String, Object> item = new HashMap<>();
            item.put("date", dateKey);
            item.put("completed", completed);
            item.put("scanned", scanned);
            result.add(item);
        }
        return result;
    }

    public Map<String, Long> getStatusStats() {
        long total = taskRepository.count();
        long completed = taskRepository.countByCompleted(true);
        long pending = total - completed;
        long needVisit = taskRepository.countByNeedVisit(true);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("已完成", completed);
        result.put("待回收", pending);
        result.put("需上门", needVisit);
        return result;
    }

    public List<Map<String, Object>> getAreaStats() {
        List<Object[]> rows = taskRepository.countGroupByArea();
        return rows.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("area", row[0] != null ? row[0] : "未分配");
            item.put("count", row[1]);
            return item;
        }).collect(Collectors.toList());
    }
}
