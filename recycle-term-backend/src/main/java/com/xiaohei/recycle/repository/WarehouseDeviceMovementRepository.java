package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WarehouseDeviceMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseDeviceMovementRepository extends JpaRepository<WarehouseDeviceMovement, Long> {
    List<WarehouseDeviceMovement> findByDeviceIdOrderByIdAsc(Long deviceId);
}
