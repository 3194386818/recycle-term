package com.xiaohei.recycle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohei.recycle.dto.WarehouseChangeRequestCreateDto;
import com.xiaohei.recycle.entity.WarehouseChangeRequest;
import com.xiaohei.recycle.entity.WarehouseItem;
import com.xiaohei.recycle.repository.WarehouseChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseChangeRequestService {

    private final WarehouseChangeRequestRepository requestRepository;
    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper;

    @Transactional
    public WarehouseChangeRequest create(WarehouseChangeRequestCreateDto dto) {
        if (dto.getWarehouseItemId() == null) {
            throw new RuntimeException("缺少工单ID");
        }
        if (dto.getRequestType() == null || dto.getRequestType().isBlank()) {
            throw new RuntimeException("缺少申请类型");
        }
        warehouseService.getById(dto.getWarehouseItemId());

        WarehouseChangeRequest req = new WarehouseChangeRequest();
        req.setWarehouseItemId(dto.getWarehouseItemId());
        req.setRequestType(dto.getRequestType().toUpperCase());
        req.setRequestContent(dto.getRequestContent());
        req.setStatus("PENDING");
        req.setRequestedBy(dto.getRequestedBy());
        return requestRepository.save(req);
    }

    public List<WarehouseChangeRequest> listAll() {
        return requestRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public WarehouseChangeRequest review(Long requestId, boolean approved, String rejectReason, String reviewer) {
        WarehouseChangeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        if (approved) {
            applyRequest(req);
            req.setStatus("APPROVED");
            req.setRejectReason(null);
        } else {
            req.setStatus("REJECTED");
            req.setRejectReason(rejectReason);
        }
        req.setReviewedBy(reviewer);
        req.setReviewedAt(LocalDateTime.now());
        return requestRepository.save(req);
    }

    private void applyRequest(WarehouseChangeRequest req) {
        String type = req.getRequestType();
        if ("DELETE".equals(type)) {
            warehouseService.delete(req.getWarehouseItemId());
            return;
        }
        if ("UPDATE".equals(type)) {
            try {
                Map<String, Object> map = objectMapper.readValue(req.getRequestContent(), Map.class);
                WarehouseItem item = new WarehouseItem();
                item.setProductId((String) map.get("productId"));
                item.setDevices((String) map.get("devices"));
                item.setCustomerName((String) map.get("customerName"));
                item.setPhone((String) map.get("phone"));
                item.setSplitter((String) map.get("splitter"));
                item.setAddress((String) map.get("address"));
                item.setSnNumber((String) map.get("snNumber"));
                item.setAccessRoom((String) map.get("accessRoom"));
                warehouseService.update(req.getWarehouseItemId(), item);
            } catch (Exception e) {
                throw new RuntimeException("申请内容解析失败");
            }
            return;
        }
        throw new RuntimeException("不支持的申请类型");
    }
}
