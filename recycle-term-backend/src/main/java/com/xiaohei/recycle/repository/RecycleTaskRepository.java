package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.RecycleTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecycleTaskRepository extends JpaRepository<RecycleTask, Long>, JpaSpecificationExecutor<RecycleTask> {

    List<RecycleTask> findByPhoneNumber(String phoneNumber);

    long countByCompleted(boolean completed);

    long countByNeedVisit(boolean needVisit);

    long countByCompletedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t.area, COUNT(t) FROM RecycleTask t GROUP BY t.area ORDER BY COUNT(t) DESC")
    List<Object[]> countGroupByArea();
}
