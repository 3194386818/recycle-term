package com.xiaohei.recycle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohei.recycle.dto.*;
import com.xiaohei.recycle.entity.Warehouse;
import com.xiaohei.recycle.entity.WarehouseDevice;
import com.xiaohei.recycle.entity.WarehouseDeviceMovement;
import com.xiaohei.recycle.entity.WarehouseItem;
import com.xiaohei.recycle.repository.WarehouseDeviceMovementRepository;
import com.xiaohei.recycle.repository.WarehouseDeviceRepository;
import com.xiaohei.recycle.repository.WarehouseItemRepository;
import com.xiaohei.recycle.repository.WarehouseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseItemRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseDeviceRepository deviceRepository;
    private final WarehouseDeviceMovementRepository movementRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @Transactional
    public void initializeWarehouseData() {
        Warehouse defaultWarehouse = getOrCreateDefaultWarehouse();
        if (deviceRepository.count() > 0) {
            return;
        }
        List<WarehouseItem> items = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        for (WarehouseItem item : items) {
            List<Map<String, Object>> legacyDevices = parseLegacyDevices(item.getDevices());
            for (Map<String, Object> legacy : legacyDevices) {
                String sn = stringValue(legacy.get("sn"));
                if (!StringUtils.hasText(sn)) {
                    continue;
                }
                WarehouseDevice device = new WarehouseDevice();
                device.setWarehouseItemId(item.getId());
                device.setWarehouseId(defaultWarehouse.getId());
                device.setType(stringValue(legacy.get("type")));
                device.setSn(sn);
                device.setInboundAt(item.getReceivedAt());
                boolean outbound = Boolean.TRUE.equals(legacy.get("outbound"));
                device.setOutbound(outbound);
                device.setStatus(outbound ? WarehouseDevice.STATUS_OUTBOUND : WarehouseDevice.STATUS_IN_STOCK);
                device.setOutboundAt(parseDateTime(legacy.get("outboundAt")));
                WarehouseDevice saved = deviceRepository.save(device);
                appendMovement(saved.getId(), null, defaultWarehouse.getId(), WarehouseDeviceMovement.TYPE_INBOUND, "migration", "旧设备数据迁移");
                if (outbound) {
                    appendMovement(saved.getId(), defaultWarehouse.getId(), null, WarehouseDeviceMovement.TYPE_OUTBOUND, "migration", "旧设备出库状态迁移");
                }
            }
            syncLegacySnapshot(item.getId());
        }
    }

    public Page<WarehouseItemDto> search(String keyword, Long warehouseId, boolean includeDisabled, boolean outboundOnly, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<WarehouseItem> allItems = repository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"));
        Map<Long, Warehouse> warehouseMap = getWarehouseMap();
        List<WarehouseItemDto> matched = new ArrayList<>();
        for (WarehouseItem item : allItems) {
            List<WarehouseDeviceDto> visibleDevices = getVisibleDeviceDtos(item.getId(), warehouseId, includeDisabled, outboundOnly, warehouseMap);
            boolean itemMatches = !StringUtils.hasText(normalizedKeyword) || itemMatches(item, normalizedKeyword);
            boolean deviceMatches = visibleDevices.stream().anyMatch(d -> deviceMatches(d, normalizedKeyword));
            if (visibleDevices.isEmpty()) {
                continue;
            }
            if (!StringUtils.hasText(normalizedKeyword) || itemMatches || deviceMatches) {
                matched.add(toItemDto(item, visibleDevices));
            }
        }
        int start = Math.min((int) pageable.getOffset(), matched.size());
        int end = Math.min(start + pageable.getPageSize(), matched.size());
        return new PageImpl<>(matched.subList(start, end), pageable, matched.size());
    }

    public WarehouseItemDto getDtoById(Long id, boolean includeDisabled) {
        WarehouseItem item = getById(id);
        return toItemDto(item, getVisibleDeviceDtos(id, null, includeDisabled, false, getWarehouseMap()));
    }

    public WarehouseItem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
    }

    @Transactional
    public WarehouseItemDto create(WarehouseItemDto dto) {
        Long targetWarehouseId = dto.getDevices() == null || dto.getDevices().isEmpty()
                ? resolveWarehouseId(null, false)
                : resolveWarehouseId(dto.getDevices().get(0).getWarehouseId(), false);
        WarehouseItem item = new WarehouseItem();
        applyItemFields(item, dto);
        WarehouseItem saved = repository.save(item);
        if (dto.getDevices() != null) {
            for (WarehouseDeviceDto deviceDto : dto.getDevices()) {
                if (!StringUtils.hasText(deviceDto.getSn())) {
                    continue;
                }
                WarehouseDevice device = new WarehouseDevice();
                device.setWarehouseItemId(saved.getId());
                device.setWarehouseId(deviceDto.getWarehouseId() != null ? resolveWarehouseId(deviceDto.getWarehouseId(), false) : targetWarehouseId);
                device.setType(deviceDto.getType());
                device.setSn(deviceDto.getSn().trim());
                device.setInboundAt(LocalDateTime.now());
                WarehouseDevice created = deviceRepository.save(device);
                appendMovement(created.getId(), null, created.getWarehouseId(), WarehouseDeviceMovement.TYPE_INBOUND, "system", "入库");
            }
        }
        syncLegacySnapshot(saved.getId());
        return getDtoById(saved.getId(), true);
    }

    @Transactional
    public WarehouseItemDto update(Long id, WarehouseItemDto dto) {
        WarehouseItem existing = getById(id);
        applyItemFields(existing, dto);
        repository.save(existing);
        if (dto.getDevices() != null) {
            for (WarehouseDeviceDto deviceDto : dto.getDevices()) {
                if (!StringUtils.hasText(deviceDto.getSn())) {
                    continue;
                }
                WarehouseDevice device = deviceDto.getId() != null
                        ? deviceRepository.findById(deviceDto.getId()).orElse(new WarehouseDevice())
                        : new WarehouseDevice();
                if (device.getId() != null && !Objects.equals(device.getWarehouseItemId(), id)) {
                    throw new RuntimeException("设备不属于该订单");
                }
                boolean isNew = device.getId() == null;
                device.setWarehouseItemId(id);
                if (isNew) {
                    device.setWarehouseId(resolveWarehouseId(deviceDto.getWarehouseId(), false));
                    device.setInboundAt(LocalDateTime.now());
                }
                device.setType(deviceDto.getType());
                device.setSn(deviceDto.getSn().trim());
                WarehouseDevice saved = deviceRepository.save(device);
                if (isNew) {
                    appendMovement(saved.getId(), null, saved.getWarehouseId(), WarehouseDeviceMovement.TYPE_INBOUND, "system", "追加设备入库");
                }
            }
        }
        syncLegacySnapshot(id);
        return getDtoById(id, true);
    }

    @Transactional
    public void delete(Long id) {
        if (deviceRepository.countByWarehouseItemId(id) > 0) {
            throw new RuntimeException("该订单下还有设备，不能直接删除");
        }
        repository.deleteById(id);
    }

    @Transactional
    public WarehouseItemDto outboundDevice(Long itemId, String sn) {
        WarehouseDevice device = deviceRepository.findByWarehouseItemIdOrderByIdAsc(itemId).stream()
                .filter(d -> sn.equals(d.getSn()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到该串码设备"));
        outboundDeviceById(device.getId());
        return getDtoById(itemId, true);
    }

    @Transactional
    public WarehouseDeviceDto outboundDeviceById(Long deviceId) {
        WarehouseDevice device = getDevice(deviceId);
        if (Boolean.TRUE.equals(device.getOutbound())) {
            throw new RuntimeException("设备已出库");
        }
        Long fromWarehouseId = device.getWarehouseId();
        LocalDateTime now = LocalDateTime.now();
        device.setOutbound(true);
        device.setStatus(WarehouseDevice.STATUS_OUTBOUND);
        device.setOutboundAt(now);
        WarehouseDevice saved = deviceRepository.save(device);
        appendMovement(saved.getId(), fromWarehouseId, null, WarehouseDeviceMovement.TYPE_OUTBOUND, "system", "设备出库");
        syncLegacySnapshot(saved.getWarehouseItemId());
        return toDeviceDto(saved, getWarehouseMap());
    }

    @Transactional
    public WarehouseDeviceDto transferDevice(Long deviceId, Long toWarehouseId, String remark) {
        WarehouseDevice device = getDevice(deviceId);
        if (Boolean.TRUE.equals(device.getOutbound())) {
            throw new RuntimeException("已出库设备不能转仓");
        }
        Long targetWarehouseId = resolveWarehouseId(toWarehouseId, false);
        if (Objects.equals(device.getWarehouseId(), targetWarehouseId)) {
            throw new RuntimeException("设备已在目标仓库");
        }
        Long fromWarehouseId = device.getWarehouseId();
        device.setWarehouseId(targetWarehouseId);
        WarehouseDevice saved = deviceRepository.save(device);
        appendMovement(saved.getId(), fromWarehouseId, targetWarehouseId, WarehouseDeviceMovement.TYPE_TRANSFER, "system", remark);
        syncLegacySnapshot(saved.getWarehouseItemId());
        return toDeviceDto(saved, getWarehouseMap());
    }

    @Transactional
    public List<WarehouseDeviceDto> transferDevices(List<Long> deviceIds, Long toWarehouseId, String remark) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new RuntimeException("请选择设备");
        }
        List<WarehouseDeviceDto> result = new ArrayList<>();
        for (Long deviceId : deviceIds) {
            result.add(transferDevice(deviceId, toWarehouseId, remark));
        }
        return result;
    }

    public List<WarehouseDeviceMovementDto> listMovements(Long deviceId) {
        Map<Long, Warehouse> warehouseMap = getWarehouseMap();
        return movementRepository.findByDeviceIdOrderByIdAsc(deviceId).stream()
                .map(m -> toMovementDto(m, warehouseMap))
                .toList();
    }

    public List<WarehouseDto> listWarehouses(boolean includeDisabled) {
        List<Warehouse> warehouses = includeDisabled ? warehouseRepository.findAllByOrderByIdAsc() : warehouseRepository.findByEnabledTrueOrderByIdAsc();
        return warehouses.stream().map(this::toWarehouseDto).toList();
    }

    @Transactional
    public WarehouseDto createWarehouse(WarehouseCreateDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("仓库名称不能为空");
        }
        if (warehouseRepository.existsByName(dto.getName().trim())) {
            throw new RuntimeException("仓库名称已存在");
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName().trim());
        warehouse.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        warehouse.setDefaultWarehouse(false);
        return toWarehouseDto(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseDto updateWarehouse(Long id, WarehouseCreateDto dto) {
        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("仓库不存在"));
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("仓库名称不能为空");
        }
        warehouse.setName(dto.getName().trim());
        if (dto.getEnabled() != null && !Boolean.TRUE.equals(warehouse.getDefaultWarehouse())) {
            warehouse.setEnabled(dto.getEnabled());
        }
        return toWarehouseDto(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseDto setWarehouseEnabled(Long id, boolean enabled) {
        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("仓库不存在"));
        if (Boolean.TRUE.equals(warehouse.getDefaultWarehouse()) && !enabled) {
            throw new RuntimeException("默认仓库不能禁用");
        }
        warehouse.setEnabled(enabled);
        return toWarehouseDto(warehouseRepository.save(warehouse));
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("仓库不存在"));
        if (Boolean.TRUE.equals(warehouse.getDefaultWarehouse())) {
            throw new RuntimeException("默认仓库不能删除");
        }
        if (deviceRepository.countByWarehouseIdAndStatus(id, WarehouseDevice.STATUS_IN_STOCK) > 0) {
            throw new RuntimeException("仓库下还有设备，请先出库或转移后再删除");
        }
        warehouseRepository.deleteById(id);
    }

    private WarehouseDevice getDevice(Long deviceId) {
        return deviceRepository.findById(deviceId).orElseThrow(() -> new RuntimeException("设备不存在"));
    }

    private Long resolveWarehouseId(Long warehouseId, boolean includeDisabled) {
        Warehouse warehouse = warehouseId == null
                ? getOrCreateDefaultWarehouse()
                : warehouseRepository.findById(warehouseId).orElseThrow(() -> new RuntimeException("仓库不存在"));
        if (!includeDisabled && !Boolean.TRUE.equals(warehouse.getEnabled())) {
            throw new RuntimeException("目标仓库已禁用");
        }
        return warehouse.getId();
    }

    private Warehouse getOrCreateDefaultWarehouse() {
        return warehouseRepository.findFirstByDefaultWarehouseTrueOrderByIdAsc().orElseGet(() -> {
            Warehouse warehouse = new Warehouse();
            warehouse.setName("默认仓库");
            warehouse.setEnabled(true);
            warehouse.setDefaultWarehouse(true);
            return warehouseRepository.save(warehouse);
        });
    }

    private void applyItemFields(WarehouseItem item, WarehouseItemDto dto) {
        if (dto.getProductId() != null) item.setProductId(dto.getProductId());
        if (dto.getCustomerName() != null) item.setCustomerName(dto.getCustomerName());
        if (dto.getPhone() != null) item.setPhone(dto.getPhone());
        if (dto.getSplitter() != null) item.setSplitter(dto.getSplitter());
        if (dto.getAddress() != null) item.setAddress(dto.getAddress());
        if (dto.getSnNumber() != null) item.setSnNumber(dto.getSnNumber());
        if (dto.getAccessRoom() != null) item.setAccessRoom(dto.getAccessRoom());
    }

    private WarehouseItemDto toItemDto(WarehouseItem item, List<WarehouseDeviceDto> devices) {
        WarehouseItemDto dto = new WarehouseItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setCustomerName(item.getCustomerName());
        dto.setPhone(item.getPhone());
        dto.setSplitter(item.getSplitter());
        dto.setAddress(item.getAddress());
        dto.setSnNumber(item.getSnNumber());
        dto.setAccessRoom(item.getAccessRoom());
        dto.setOutbound(item.getOutbound());
        dto.setOutboundAt(item.getOutboundAt());
        dto.setReceivedAt(item.getReceivedAt());
        dto.setDevices(devices);
        return dto;
    }

    private List<WarehouseDeviceDto> getVisibleDeviceDtos(Long itemId, Long warehouseId, boolean includeDisabled, boolean outboundOnly, Map<Long, Warehouse> warehouseMap) {
        return deviceRepository.findByWarehouseItemIdOrderByIdAsc(itemId).stream()
                .filter(device -> warehouseId == null || Objects.equals(device.getWarehouseId(), warehouseId))
                .filter(device -> includeDisabled || Boolean.TRUE.equals(Optional.ofNullable(warehouseMap.get(device.getWarehouseId())).map(Warehouse::getEnabled).orElse(false)))
                .filter(device -> !outboundOnly || Boolean.TRUE.equals(device.getOutbound()))
                .map(device -> toDeviceDto(device, warehouseMap))
                .toList();
    }

    private WarehouseDeviceDto toDeviceDto(WarehouseDevice device, Map<Long, Warehouse> warehouseMap) {
        WarehouseDeviceDto dto = new WarehouseDeviceDto();
        dto.setId(device.getId());
        dto.setWarehouseItemId(device.getWarehouseItemId());
        dto.setWarehouseId(device.getWarehouseId());
        Warehouse warehouse = warehouseMap.get(device.getWarehouseId());
        dto.setWarehouseName(warehouse == null ? null : warehouse.getName());
        dto.setType(device.getType());
        dto.setSn(device.getSn());
        dto.setStatus(device.getStatus());
        dto.setOutbound(device.getOutbound());
        dto.setInboundAt(device.getInboundAt());
        dto.setOutboundAt(device.getOutboundAt());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedAt(device.getUpdatedAt());
        return dto;
    }

    private WarehouseDeviceMovementDto toMovementDto(WarehouseDeviceMovement movement, Map<Long, Warehouse> warehouseMap) {
        WarehouseDeviceMovementDto dto = new WarehouseDeviceMovementDto();
        dto.setId(movement.getId());
        dto.setDeviceId(movement.getDeviceId());
        dto.setFromWarehouseId(movement.getFromWarehouseId());
        dto.setFromWarehouseName(Optional.ofNullable(warehouseMap.get(movement.getFromWarehouseId())).map(Warehouse::getName).orElse(null));
        dto.setToWarehouseId(movement.getToWarehouseId());
        dto.setToWarehouseName(Optional.ofNullable(warehouseMap.get(movement.getToWarehouseId())).map(Warehouse::getName).orElse(null));
        dto.setMovementType(movement.getMovementType());
        dto.setOperator(movement.getOperator());
        dto.setRemark(movement.getRemark());
        dto.setCreatedAt(movement.getCreatedAt());
        return dto;
    }

    private WarehouseDto toWarehouseDto(Warehouse warehouse) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setEnabled(warehouse.getEnabled());
        dto.setDefaultWarehouse(warehouse.getDefaultWarehouse());
        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());
        return dto;
    }

    private Map<Long, Warehouse> getWarehouseMap() {
        return warehouseRepository.findAll().stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private boolean itemMatches(WarehouseItem item, String keyword) {
        return contains(item.getProductId(), keyword)
                || contains(item.getCustomerName(), keyword)
                || contains(item.getPhone(), keyword)
                || contains(item.getSnNumber(), keyword)
                || contains(item.getSplitter(), keyword)
                || contains(item.getAddress(), keyword)
                || contains(item.getAccessRoom(), keyword);
    }

    private boolean deviceMatches(WarehouseDeviceDto device, String keyword) {
        return contains(device.getSn(), keyword)
                || contains(device.getType(), keyword)
                || contains(device.getWarehouseName(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void appendMovement(Long deviceId, Long fromWarehouseId, Long toWarehouseId, String type, String operator, String remark) {
        WarehouseDeviceMovement movement = new WarehouseDeviceMovement();
        movement.setDeviceId(deviceId);
        movement.setFromWarehouseId(fromWarehouseId);
        movement.setToWarehouseId(toWarehouseId);
        movement.setMovementType(type);
        movement.setOperator(operator);
        movement.setRemark(remark);
        movementRepository.save(movement);
    }

    private void syncLegacySnapshot(Long itemId) {
        WarehouseItem item = repository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }
        List<Map<String, Object>> devices = deviceRepository.findByWarehouseItemIdOrderByIdAsc(itemId).stream().map(device -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", device.getId());
            map.put("type", device.getType());
            map.put("sn", device.getSn());
            map.put("warehouseId", device.getWarehouseId());
            map.put("outbound", device.getOutbound());
            map.put("outboundAt", device.getOutboundAt());
            return map;
        }).toList();
        try {
            item.setDevices(objectMapper.writeValueAsString(devices));
            boolean allOutbound = !devices.isEmpty() && devices.stream().allMatch(d -> Boolean.TRUE.equals(d.get("outbound")));
            item.setOutbound(allOutbound);
            item.setOutboundAt(allOutbound ? LocalDateTime.now() : null);
            repository.save(item);
        } catch (Exception e) {
            throw new RuntimeException("同步设备快照失败");
        }
    }

    private List<Map<String, Object>> parseLegacyDevices(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
