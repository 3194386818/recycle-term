package com.xiaohei.recycle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.entity.WarehouseItem;
import com.xiaohei.recycle.entity.WorkOrder;
import com.xiaohei.recycle.entity.WorkOrderOption;
import com.xiaohei.recycle.repository.WarehouseItemRepository;
import com.xiaohei.recycle.repository.WorkOrderOptionRepository;
import com.xiaohei.recycle.repository.WorkOrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    public static final String SOURCE_WAREHOUSE_LEGACY = "WAREHOUSE_LEGACY";

    private final WorkOrderRepository workOrderRepository;
    private final WarehouseItemRepository warehouseItemRepository;
    private final WorkOrderOptionRepository optionRepository;
    private final WorkOrderQuickParseService quickParseService;
    private final ObjectMapper objectMapper;

    public Page<WorkOrder> search(String keyword, String status, String type, Pageable pageable) {
        Specification<WorkOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("workOrderNo"), like),
                        cb.like(root.get("productId"), like),
                        cb.like(root.get("userName"), like),
                        cb.like(root.get("contactPhone"), like),
                        cb.like(root.get("address"), like),
                        cb.like(root.get("onuSn"), like),
                        cb.like(root.get("splitter"), like)
                ));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("workOrderType"), type.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return workOrderRepository.findAll(spec, pageable);
    }

    public WorkOrder getById(Long id) {
        return workOrderRepository.findById(id).orElseThrow(() -> new RuntimeException("工单不存在"));
    }

    public WorkOrderDto quickParse(String text) {
        return quickParseService.parse(text);
    }

    @Transactional
    public WorkOrder quickCreate(String text) {
        WorkOrderDto dto = quickParse(text);
        dto.setSourceType("QUICK_TEXT");
        dto.setRawSource(text);
        return create(dto);
    }

    @Transactional
    public WorkOrder create(WorkOrderDto dto) {
        WorkOrder workOrder = new WorkOrder();
        copyDto(dto, workOrder, false);
        workOrder.setWorkOrderNo(resolveWorkOrderNo(dto == null ? null : dto.getWorkOrderNo()));
        if (!StringUtils.hasText(workOrder.getStatus())) {
            workOrder.setStatus(WorkOrder.STATUS_ACCEPTED);
        }
        validateStatus(workOrder.getStatus());
        validateOptions(workOrder.getWorkOrderType(), workOrder.getFailureReason());
        validateFailed(workOrder.getStatus(), workOrder.getFailureReason());
        applyStatusTimestamp(workOrder, workOrder.getStatus(), LocalDateTime.now());
        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public WorkOrder update(Long id, WorkOrderDto dto) {
        WorkOrder workOrder = getById(id);
        String oldStatus = workOrder.getStatus();
        if (dto != null && dto.getWorkOrderNo() != null && !dto.getWorkOrderNo().equals(workOrder.getWorkOrderNo())) {
            validateWorkOrderNo(dto.getWorkOrderNo());
            if (workOrderRepository.existsByWorkOrderNo(dto.getWorkOrderNo())) {
                throw new RuntimeException("工单号已存在");
            }
            workOrder.setWorkOrderNo(dto.getWorkOrderNo());
        }
        copyDto(dto, workOrder, true);
        validateStatus(workOrder.getStatus());
        validateOptions(workOrder.getWorkOrderType(), workOrder.getFailureReason());
        if (!Objects.equals(oldStatus, workOrder.getStatus())) {
            validateTransition(oldStatus, workOrder.getStatus());
            applyStatusTimestamp(workOrder, workOrder.getStatus(), LocalDateTime.now());
        }
        validateFailed(workOrder.getStatus(), workOrder.getFailureReason());
        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public WorkOrder updateStatus(Long id, WorkOrderStatusDto dto) {
        WorkOrder workOrder = getById(id);
        if (dto == null || !StringUtils.hasText(dto.getStatus())) {
            throw new RuntimeException("工单状态不能为空");
        }
        String target = dto.getStatus().trim();
        validateStatus(target);
        validateTransition(workOrder.getStatus(), target);
        if (dto.getFailureReason() != null) {
            workOrder.setFailureReason(trim(dto.getFailureReason()));
        }
        if (dto.getRemark() != null) {
            workOrder.setRemark(dto.getRemark());
        }
        validateFailed(target, workOrder.getFailureReason());
        validateFailureReason(workOrder.getFailureReason());
        workOrder.setStatus(target);
        applyStatusTimestamp(workOrder, target, LocalDateTime.now());
        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public void delete(Long id) {
        if (!workOrderRepository.existsById(id)) {
            throw new RuntimeException("工单不存在");
        }
        workOrderRepository.deleteById(id);
    }

    public WorkOrderLegacyImportPreviewDto previewLegacyImport(int sampleSize) {
        List<WarehouseItem> items = warehouseItemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        long existing = 0;
        List<WorkOrderDto> samples = new ArrayList<>();
        int limit = Math.max(0, sampleSize);
        for (WarehouseItem item : items) {
            String sourceId = String.valueOf(item.getId());
            if (workOrderRepository.existsBySourceTypeAndSourceId(SOURCE_WAREHOUSE_LEGACY, sourceId)) {
                existing++;
                continue;
            }
            if (samples.size() < limit) {
                samples.add(toLegacyDto(item));
            }
        }
        return new WorkOrderLegacyImportPreviewDto(items.size(), items.size() - existing, existing, samples);
    }

    @Transactional
    public WorkOrderLegacyImportResultDto importLegacy() {
        List<WarehouseItem> items = warehouseItemRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        long created = 0;
        long skipped = 0;
        for (WarehouseItem item : items) {
            String sourceId = String.valueOf(item.getId());
            if (workOrderRepository.existsBySourceTypeAndSourceId(SOURCE_WAREHOUSE_LEGACY, sourceId)) {
                skipped++;
                continue;
            }
            WorkOrder workOrder = new WorkOrder();
            WorkOrderDto dto = toLegacyDto(item);
            copyDto(dto, workOrder, false);
            workOrder.setWorkOrderNo(generateWorkOrderNo(item.getReceivedAt()));
            workOrder.setStatus(WorkOrder.STATUS_ACCEPTED);
            workOrderRepository.save(workOrder);
            created++;
        }
        return new WorkOrderLegacyImportResultDto(items.size(), created, skipped);
    }

    private WorkOrderDto toLegacyDto(WarehouseItem item) {
        WorkOrderDto dto = new WorkOrderDto();
        dto.setProductId(item.getProductId());
        dto.setUserName(item.getCustomerName());
        dto.setContactPhone(item.getPhone());
        dto.setSplitter(item.getSplitter());
        dto.setAddress(item.getAddress());
        dto.setOnuSn(item.getSnNumber());
        dto.setRemark(StringUtils.hasText(item.getAccessRoom()) ? "接入间：" + item.getAccessRoom() : null);
        dto.setSourceType(SOURCE_WAREHOUSE_LEGACY);
        dto.setSourceId(String.valueOf(item.getId()));
        dto.setRawSource(toRawSource(item));
        return dto;
    }

    private void copyDto(WorkOrderDto dto, WorkOrder workOrder, boolean partial) {
        if (dto == null) return;
        if (!partial || dto.getProductId() != null) workOrder.setProductId(trim(dto.getProductId()));
        if (!partial || dto.getUserName() != null) workOrder.setUserName(trim(dto.getUserName()));
        if (!partial || dto.getContactPhone() != null) workOrder.setContactPhone(trim(dto.getContactPhone()));
        if (!partial || dto.getWorkOrderType() != null) workOrder.setWorkOrderType(trim(dto.getWorkOrderType()));
        if (!partial || dto.getAddress() != null) workOrder.setAddress(trim(dto.getAddress()));
        if (!partial || dto.getCvlan() != null) workOrder.setCvlan(trim(dto.getCvlan()));
        if (!partial || dto.getSvlan() != null) workOrder.setSvlan(trim(dto.getSvlan()));
        if (!partial || dto.getSplitter() != null) workOrder.setSplitter(trim(dto.getSplitter()));
        if (!partial || dto.getSplitterPort() != null) workOrder.setSplitterPort(trim(dto.getSplitterPort()));
        if (!partial || dto.getOnuSn() != null) workOrder.setOnuSn(trim(dto.getOnuSn()));
        if (!partial || dto.getStatus() != null) workOrder.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : WorkOrder.STATUS_ACCEPTED);
        if (!partial || dto.getFailureReason() != null) workOrder.setFailureReason(trim(dto.getFailureReason()));
        if (!partial || dto.getRemark() != null) workOrder.setRemark(trim(dto.getRemark()));
        if (!partial || dto.getAppointedAt() != null) workOrder.setAppointedAt(dto.getAppointedAt());
        if (!partial || dto.getFulfilledAt() != null) workOrder.setFulfilledAt(dto.getFulfilledAt());
        if (!partial || dto.getCompletedAt() != null) workOrder.setCompletedAt(dto.getCompletedAt());
        if (!partial || dto.getTransferredAt() != null) workOrder.setTransferredAt(dto.getTransferredAt());
        if (!partial || dto.getFailedAt() != null) workOrder.setFailedAt(dto.getFailedAt());
        if (!partial || dto.getSourceType() != null) workOrder.setSourceType(trim(dto.getSourceType()));
        if (!partial || dto.getSourceId() != null) workOrder.setSourceId(trim(dto.getSourceId()));
        if (!partial || dto.getRawSource() != null) workOrder.setRawSource(dto.getRawSource());
    }

    private String resolveWorkOrderNo(String workOrderNo) {
        if (StringUtils.hasText(workOrderNo)) {
            String normalized = workOrderNo.trim();
            validateWorkOrderNo(normalized);
            if (workOrderRepository.existsByWorkOrderNo(normalized)) {
                throw new RuntimeException("工单号已存在");
            }
            return normalized;
        }
        return generateWorkOrderNo(LocalDateTime.now());
    }

    private String generateWorkOrderNo(LocalDateTime time) {
        String prefix = Optional.ofNullable(time).orElse(LocalDateTime.now()).format(DateTimeFormatter.BASIC_ISO_DATE);
        long next = workOrderRepository.countByWorkOrderNoStartingWith(prefix) + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%08d", next++);
        } while (workOrderRepository.existsByWorkOrderNo(candidate));
        return candidate;
    }

    private void validateWorkOrderNo(String workOrderNo) {
        if (!workOrderNo.matches("\\d{16,17}")) {
            throw new RuntimeException("工单号必须为16或17位数字");
        }
        try {
            LocalDate.parse(workOrderNo.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("工单号日期无效");
        }
    }

    private void validateStatus(String status) {
        if (!Set.of(
                WorkOrder.STATUS_ACCEPTED,
                WorkOrder.STATUS_APPOINTED,
                WorkOrder.STATUS_FULFILLING,
                WorkOrder.STATUS_COMPLETED,
                WorkOrder.STATUS_TRANSFERRED,
                WorkOrder.STATUS_FAILED
        ).contains(status)) {
            throw new RuntimeException("工单状态无效");
        }
    }

    private void validateTransition(String from, String to) {
        if (Objects.equals(from, to)) {
            return;
        }
        boolean valid = switch (from) {
            case WorkOrder.STATUS_ACCEPTED -> Set.of(WorkOrder.STATUS_APPOINTED, WorkOrder.STATUS_FULFILLING, WorkOrder.STATUS_TRANSFERRED, WorkOrder.STATUS_FAILED).contains(to);
            case WorkOrder.STATUS_APPOINTED -> Set.of(WorkOrder.STATUS_FULFILLING, WorkOrder.STATUS_TRANSFERRED, WorkOrder.STATUS_FAILED).contains(to);
            case WorkOrder.STATUS_FULFILLING -> Set.of(WorkOrder.STATUS_COMPLETED, WorkOrder.STATUS_TRANSFERRED, WorkOrder.STATUS_FAILED).contains(to);
            default -> false;
        };
        if (!valid) {
            throw new RuntimeException("不允许从" + from + "切换到" + to);
        }
    }

    private void validateFailed(String status, String failureReason) {
        if (WorkOrder.STATUS_FAILED.equals(status) && !StringUtils.hasText(failureReason)) {
            throw new RuntimeException("失败工单必须填写失败原因");
        }
    }

    private void validateOptions(String workOrderType, String failureReason) {
        validateWorkOrderType(workOrderType);
        validateFailureReason(failureReason);
    }

    private void validateWorkOrderType(String workOrderType) {
        if (StringUtils.hasText(workOrderType)
                && !optionRepository.existsByCategoryAndValueAndEnabledTrue(WorkOrderOption.CATEGORY_TYPE, workOrderType.trim())) {
            throw new RuntimeException("工单类型无效");
        }
    }

    private void validateFailureReason(String failureReason) {
        if (StringUtils.hasText(failureReason)
                && !optionRepository.existsByCategoryAndValueAndEnabledTrue(WorkOrderOption.CATEGORY_FAILURE_REASON, failureReason.trim())) {
            throw new RuntimeException("失败原因无效");
        }
    }

    private void applyStatusTimestamp(WorkOrder workOrder, String status, LocalDateTime now) {
        switch (status) {
            case WorkOrder.STATUS_APPOINTED -> workOrder.setAppointedAt(now);
            case WorkOrder.STATUS_FULFILLING -> workOrder.setFulfilledAt(now);
            case WorkOrder.STATUS_COMPLETED -> workOrder.setCompletedAt(now);
            case WorkOrder.STATUS_TRANSFERRED -> workOrder.setTransferredAt(now);
            case WorkOrder.STATUS_FAILED -> workOrder.setFailedAt(now);
            default -> { }
        }
    }

    private String toRawSource(WarehouseItem item) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("id", item.getId());
        raw.put("productId", item.getProductId());
        raw.put("customerName", item.getCustomerName());
        raw.put("phone", item.getPhone());
        raw.put("splitter", item.getSplitter());
        raw.put("address", item.getAddress());
        raw.put("snNumber", item.getSnNumber());
        raw.put("accessRoom", item.getAccessRoom());
        raw.put("receivedAt", item.getReceivedAt());
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            return String.valueOf(raw);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
