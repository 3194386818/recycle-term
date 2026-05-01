package com.xiaohei.recycle.repository;

import com.xiaohei.recycle.entity.RecycleTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecycleTaskRepository extends JpaRepository<RecycleTask, Long>, JpaSpecificationExecutor<RecycleTask> {

    Page<RecycleTask> findByPhoneNumberLikeOrUserNameLikeOrUserAddressLike(
            String phone, String name, String address, Pageable pageable);

    long countByCompleted(boolean completed);

    long countByNeedVisit(boolean needVisit);

    long countByCompletedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t.area, COUNT(t) FROM RecycleTask t GROUP BY t.area ORDER BY COUNT(t) DESC")
    List<Object[]> countGroupByArea();

    @Query("SELECT t FROM RecycleTask t WHERE t.phoneNumber LIKE ?1 OR t.userName LIKE ?2 OR t.userAddress LIKE ?3 OR t.terminals LIKE ?4")
    Page<RecycleTask> searchByKeyword(String phone, String name, String address, String terminals, Pageable pageable);

    default Page<RecycleTask> searchByKeyword(String keyword, Pageable pageable) {
        return searchByKeyword(keyword, keyword, keyword, keyword, pageable);
    }
}
