package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WarehouseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WarehouseItemRepository extends JpaRepository<WarehouseItem, Long>, JpaSpecificationExecutor<WarehouseItem> {
}
