package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.entity.RecycleTask;
import com.xiaohei.recycle.service.ExcelImportService;
import com.xiaohei.recycle.service.RecycleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;
    private final RecycleTaskService taskService;

    @PostMapping("/excel")
    public Result<List<RecycleTask>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error("请上传 Excel 文件");
        }
        try {
            List<RecycleTask> tasks = excelImportService.importFromExcel(file);
            return Result.ok("导入成功，共 " + tasks.size() + " 条", tasks);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/backfill-product-id")
    public Result<Integer> backfillProductId(@RequestBody Map<String, String> mapping) {
        int updated = taskService.backfillProductId(mapping);
        return Result.ok("回填完成，更新 " + updated + " 条", updated);
    }
}
