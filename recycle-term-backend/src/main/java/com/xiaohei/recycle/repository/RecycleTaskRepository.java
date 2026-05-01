package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.RecycleTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecycleTaskRepository extends JpaRepository<RecycleTask, Long>, JpaSpecificationExecutor<RecycleTask> {
}
