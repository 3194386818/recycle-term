package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.Engineer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EngineerRepository extends JpaRepository<Engineer, Long> {
    Optional<Engineer> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
