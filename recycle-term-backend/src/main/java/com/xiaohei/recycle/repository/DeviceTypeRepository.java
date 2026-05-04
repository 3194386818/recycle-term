package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {
    List<DeviceType> findAllByOrderByNameAsc();
    List<DeviceType> findAllByOrderByNameDesc();
    List<DeviceType> findAllByIdOrderByIdAsc();
    List<DeviceType> findAllByIdOrderByIdDesc();
    List<DeviceType> findAllByOrderByCreatedAtAsc();
    List<DeviceType> findAllByOrderByCreatedAtDesc();
    boolean existsByName(String name);
}
