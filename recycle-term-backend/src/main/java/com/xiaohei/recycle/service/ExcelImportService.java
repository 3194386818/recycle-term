package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final RecycleTaskRepository taskRepository;

    @Transactional
    public List<RecycleTask> importFromExcel(MultipartFile file) throws IOException {
        List<RecycleTask> tasks = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return tasks;

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                RecycleTask task = new RecycleTask();
                task.setPhoneNumber(getStringCell(row, 0));
                task.setEngineerName(getStringCell(row, 4));
                task.setEngineerPhone(getStringCell(row, 2));
                task.setDetailDesc(getStringCell(row, 5));
                task.setUserName(getStringCell(row, 7));
                task.setAccessRoom(getStringCell(row, 8));
                task.setCategory(getStringCell(row, 9));
                task.setDevDept(getStringCell(row, 14));
                task.setDevPerson(getStringCell(row, 15));
                task.setArea(getStringCell(row, 16));
                task.setUserAddress(getStringCell(row, 17));
                task.setNeedVisit("是".equals(getStringCell(row, 18)));
                task.setTerminals(getStringCell(row, 22));
                task.setExpectedCount(getIntCell(row, 24));
                task.setFttrCount(getIntCell(row, 25));

                tasks.add(taskRepository.save(task));
            }
        }

        log.info("Imported {} tasks from Excel", tasks.size());
        return tasks;
    }

    private String getStringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String val = cell.getStringCellValue();
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    private Integer getIntCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            cell.setCellType(CellType.STRING);
            String val = cell.getStringCellValue();
            if (val == null || val.isBlank()) return null;
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
