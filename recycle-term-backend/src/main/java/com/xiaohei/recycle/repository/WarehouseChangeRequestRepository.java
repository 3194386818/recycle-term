package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WarehouseChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseChangeRequestRepository extends JpaRepository<WarehouseChangeRequest, Long> {
    List<WarehouseChangeRequest> findAllByOrderByIdDesc();
}
