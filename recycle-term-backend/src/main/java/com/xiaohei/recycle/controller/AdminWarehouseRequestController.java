package com.xiaohei.recycle.controller;

import com.xiaohei.recycle.dto.Result;
import com.xiaohei.recycle.dto.WarehouseChangeRequestReviewDto;
import com.xiaohei.recycle.entity.WarehouseChangeRequest;
import com.xiaohei.recycle.service.WarehouseChangeRequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouse-requests")
@RequiredArgsConstructor
public class AdminWarehouseRequestController {

    private final WarehouseChangeRequestService requestService;

    @GetMapping
    public Result<List<WarehouseChangeRequest>> list() {
        return Result.ok(requestService.listAll());
    }

    @PatchMapping("/{id}/review")
    public Result<WarehouseChangeRequest> review(@PathVariable Long id,
                                                 @RequestBody WarehouseChangeRequestReviewDto dto,
                                                 HttpServletRequest req) {
        String reviewer = (String) req.getAttribute("adminUsername");
        WarehouseChangeRequest result = requestService.review(id, dto.isApproved(), dto.getRejectReason(), reviewer);
        return Result.ok(dto.isApproved() ? "已通过" : "已驳回", result);
    }
}
