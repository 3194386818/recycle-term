package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.WorkOrderOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderOptionRepository extends JpaRepository<WorkOrderOption, Long> {
    boolean existsByCategoryAndValue(String category, String value);

    boolean existsByCategoryAndValueAndEnabledTrue(String category, String value);

    List<WorkOrderOption> findByCategoryOrderBySortOrderAscIdAsc(String category);

    List<WorkOrderOption> findByCategoryAndEnabledTrueOrderBySortOrderAscIdAsc(String category);
    List<WorkOrderOption> findAllByOrderByCategoryAscSortOrderAscIdAsc();
}
