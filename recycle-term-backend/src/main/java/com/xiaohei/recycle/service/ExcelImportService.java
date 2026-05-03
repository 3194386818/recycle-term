package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
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

    public List<RecycleTask> parseExcel(MultipartFile file) throws IOException {
        return parse(file, false);
    }

    @Transactional
    public List<RecycleTask> importFromExcel(MultipartFile file) throws IOException {
        return parse(file, true);
    }

    private List<RecycleTask> parse(MultipartFile file, boolean save) throws IOException {
        List<RecycleTask> tasks = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return tasks;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                RecycleTask task = new RecycleTask();
                task.setPhoneNumber(getStringCell(row, 0));       // Col 0: 用户号码
                task.setProductId(getStringCell(row, 10));         // Col 10: 产品号(020开头)
                task.setEngineerName(getStringCell(row, 4));       // Col 4: 工程师姓名
                task.setEngineerPhone(getStringCell(row, 2));      // Col 2: 工程师手机号
                task.setDetailDesc(getStringCell(row, 8));         // Col 8: 详情说明
                task.setUserName(getStringCell(row, 11));          // Col 11: 用户名称
                task.setAccessRoom(getStringCell(row, 14));        // Col 14: 接入间名称
                task.setCategory(getStringCell(row, 15));          // Col 15: 分类/细分
                task.setDevDept(getStringCell(row, 20));           // Col 20: 发展部门
                task.setDevPerson(getStringCell(row, 21));         // Col 21: 发展员工
                task.setArea(getStringCell(row, 22));              // Col 22: 区域
                task.setUserAddress(getStringCell(row, 23));       // Col 23: 用户地址
                task.setNeedVisit("是".equals(getStringCell(row, 27))); // Col 27: 是否上门
                task.setTerminals(getStringCell(row, 31));         // Col 31: 应回收终端
                task.setExpectedCount(getIntCell(row, 33));        // Col 33: 实际应回终端数量
                task.setFttrCount(getIntCell(row, 34));            // Col 34: FTTR主光猫数量

                tasks.add(task);
            }

            if (save) {
                taskRepository.saveAll(tasks);
                log.info("Imported {} tasks from Excel", tasks.size());
            }
        }

        return tasks;
    }

    private String getStringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case STRING:
                    break;
                case NUMERIC:
                    cell.setCellType(CellType.STRING);
                    break;
                case FORMULA:
                    // Evaluate formula, get the result
                    try {
                        DataFormatter formatter = new DataFormatter();
                        return formatter.formatCellValue(cell);
                    } catch (Exception e) {
                        cell.setCellType(CellType.STRING);
                    }
                    break;
                default:
                    cell.setCellType(CellType.STRING);
            }
        } catch (Exception e) {
            cell.setCellType(CellType.STRING);
        }
        try {
            String val = cell.getStringCellValue();
            return (val != null && !val.isBlank()) ? val.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getIntCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    return (int) cell.getNumericCellValue();
                } catch (Exception ignored) {}
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
