package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findFirstByDefaultWarehouseTrueOrderByIdAsc();
    List<Warehouse> findAllByOrderByIdAsc();
    List<Warehouse> findByEnabledTrueOrderByIdAsc();
    boolean existsByName(String name);
}
