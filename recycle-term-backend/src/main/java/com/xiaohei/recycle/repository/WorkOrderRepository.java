package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    boolean existsByWorkOrderNo(String workOrderNo);

    long countByWorkOrderNoStartingWith(String prefix);

    boolean existsBySourceTypeAndSourceId(String sourceType, String sourceId);

    Optional<WorkOrder> findBySourceTypeAndSourceId(String sourceType, String sourceId);
}
