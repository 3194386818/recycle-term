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
                task.setPhoneNumber(getStringCell(row, 8));       // Col 8: 用户号码 (手机号)
                task.setProductId(getStringCell(row, 18));        // Col 18: 产品号 (020开头)
                task.setEngineerPhone(getStringCell(row, 10));    // Col 10: 工程师手机号
                task.setEngineerName(getStringCell(row, 12));     // Col 12: 工程师姓名
                task.setDetailDesc(getStringCell(row, 16));       // Col 16: 详情说明
                task.setUserName(getStringCell(row, 19));         // Col 19: 用户名称
                task.setAccessRoom(getStringCell(row, 22));       // Col 22: 接入间名称
                task.setCategory(getStringCell(row, 23));         // Col 23: 分类/细分
                task.setDevDept(getStringCell(row, 28));          // Col 28: 发展部门
                task.setDevPerson(getStringCell(row, 29));        // Col 29: 发展员工
                task.setArea(getStringCell(row, 30));             // Col 30: 区域
                task.setUserAddress(getStringCell(row, 31));      // Col 31: 用户地址
                task.setNeedVisit("是".equals(getStringCell(row, 35))); // Col 35: 是否上门
                task.setTerminals(getStringCell(row, 39));        // Col 39: 应回收终端
                // TODO: 设备类型统一后，此处改用 DeviceType API 解析终端中的设备类型
                task.setExpectedCount(getIntCell(row, 40));       // Col 40: 下发应回终端数量(原始)
                task.setFttrCount(getIntCell(row, 42));           // Col 42: 其中应回收FTTR主光猫数量

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
            return new DataFormatter().formatCellValue(cell).trim();
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
