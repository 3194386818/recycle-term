package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.WarehouseItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohei.recycle.repository.WarehouseItemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseItemRepository repository;
    private final ObjectMapper objectMapper;

    public Page<WarehouseItem> search(String keyword, Pageable pageable) {
        Specification<WarehouseItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("productId"), like),
                    cb.like(root.get("devices"), like),
                    cb.like(root.get("customerName"), like),
                    cb.like(root.get("snNumber"), like),
                    cb.like(root.get("address"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public WarehouseItem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
    }

    @Transactional
    public WarehouseItem create(WarehouseItem item) {
        return repository.save(item);
    }

    @Transactional
    public WarehouseItem update(Long id, WarehouseItem item) {
        WarehouseItem existing = getById(id);
        if (item.getProductId() != null) existing.setProductId(item.getProductId());
        if (item.getDevices() != null) existing.setDevices(item.getDevices());
        if (item.getCustomerName() != null) existing.setCustomerName(item.getCustomerName());
        if (item.getPhone() != null) existing.setPhone(item.getPhone());
        if (item.getSplitter() != null) existing.setSplitter(item.getSplitter());
        if (item.getAddress() != null) existing.setAddress(item.getAddress());
        if (item.getSnNumber() != null) existing.setSnNumber(item.getSnNumber());
        if (item.getAccessRoom() != null) existing.setAccessRoom(item.getAccessRoom());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public WarehouseItem outboundDevice(Long id, String sn) {
        if (!StringUtils.hasText(sn)) {
            throw new RuntimeException("串码不能为空");
        }
        WarehouseItem item = getById(id);

        try {
            List<java.util.Map<String, Object>> devices = objectMapper.readValue(
                    item.getDevices() == null ? "[]" : item.getDevices(),
                    new TypeReference<List<java.util.Map<String, Object>>>() {}
            );
            boolean found = false;
            LocalDateTime now = LocalDateTime.now();
            for (java.util.Map<String, Object> d : devices) {
                Object deviceSn = d.get("sn");
                if (deviceSn != null && sn.equals(String.valueOf(deviceSn))) {
                    d.put("outbound", true);
                    d.put("outboundAt", now.toString());
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("未找到该串码设备");
            }
            item.setDevices(objectMapper.writeValueAsString(devices));

            boolean allOutbound = true;
            for (java.util.Map<String, Object> d : devices) {
                Object outbound = d.get("outbound");
                if (!(outbound instanceof Boolean) || !((Boolean) outbound)) {
                    allOutbound = false;
                    break;
                }
            }
            item.setOutbound(allOutbound);
            item.setOutboundAt(allOutbound ? now : null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("设备出库处理失败");
        }
        return repository.save(item);
    }
}
