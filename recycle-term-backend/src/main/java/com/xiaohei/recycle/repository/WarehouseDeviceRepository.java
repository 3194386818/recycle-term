package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WarehouseDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WarehouseDeviceRepository extends JpaRepository<WarehouseDevice, Long> {
    List<WarehouseDevice> findByWarehouseItemIdOrderByIdAsc(Long warehouseItemId);
    List<WarehouseDevice> findByWarehouseItemIdInOrderByIdAsc(Collection<Long> warehouseItemIds);
    List<WarehouseDevice> findBySnContainingIgnoreCaseOrTypeContainingIgnoreCase(String sn, String type);
    long countByWarehouseIdAndStatus(Long warehouseId, String status);
    long countByWarehouseItemId(Long warehouseItemId);
}
