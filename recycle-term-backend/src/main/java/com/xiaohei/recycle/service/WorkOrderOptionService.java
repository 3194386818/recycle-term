package com.xiaohei.recycle.service;

import com.xiaohei.recycle.dto.WorkOrderOptionDto;
import com.xiaohei.recycle.dto.WorkOrderOptionsDto;
import com.xiaohei.recycle.entity.WorkOrder;
import com.xiaohei.recycle.entity.WorkOrderOption;
import com.xiaohei.recycle.repository.WorkOrderOptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderOptionService {

    private final WorkOrderOptionRepository repository;

    @PostConstruct
    public void init() {
        seed(WorkOrderOption.CATEGORY_TYPE, "新装", "新装", 10);
        seed(WorkOrderOption.CATEGORY_TYPE, "移机", "移机", 20);
        seed(WorkOrderOption.CATEGORY_TYPE, "故障", "故障", 30);
        seed(WorkOrderOption.CATEGORY_FAILURE_REASON, "联系不上", "联系不上", 10);
        seed(WorkOrderOption.CATEGORY_FAILURE_REASON, "地址没资源", "地址没资源", 20);
        seed(WorkOrderOption.CATEGORY_FAILURE_REASON, "用户不安装", "用户不安装", 30);
        seed(WorkOrderOption.CATEGORY_FAILURE_REASON, "发展人下错单", "发展人下错单", 40);
        seed(WorkOrderOption.CATEGORY_FAILURE_REASON, "其他", "其他", 50);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_ACCEPTED, "接单", 10);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_APPOINTED, "预约", 20);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_FULFILLING, "履约", 30);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_COMPLETED, "完成", 40);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_TRANSFERRED, "调走", 50);
        seed(WorkOrderOption.CATEGORY_STATUS, WorkOrder.STATUS_FAILED, "失败", 60);
    }

    public WorkOrderOptionsDto getOptions() {
        WorkOrderOptionsDto dto = new WorkOrderOptionsDto();
        dto.setTypes(repository.findByCategoryAndEnabledTrueOrderBySortOrderAscIdAsc(WorkOrderOption.CATEGORY_TYPE));
        dto.setFailureReasons(repository.findByCategoryAndEnabledTrueOrderBySortOrderAscIdAsc(WorkOrderOption.CATEGORY_FAILURE_REASON));
        dto.setStatuses(repository.findByCategoryAndEnabledTrueOrderBySortOrderAscIdAsc(WorkOrderOption.CATEGORY_STATUS));
        return dto;
    }

    public List<WorkOrderOption> list(String category) {
        if (StringUtils.hasText(category)) {
            return repository.findByCategoryOrderBySortOrderAscIdAsc(category.trim());
        }
        return repository.findAllByOrderByCategoryAscSortOrderAscIdAsc();
    }

    @Transactional
    public WorkOrderOption create(WorkOrderOptionDto dto) {
        validate(dto);
        String category = dto.getCategory().trim();
        String value = dto.getValue().trim();
        if (repository.existsByCategoryAndValue(category, value)) {
            throw new RuntimeException("选项已存在");
        }
        WorkOrderOption option = new WorkOrderOption();
        copy(dto, option);
        return repository.save(option);
    }

    @Transactional
    public WorkOrderOption update(Long id, WorkOrderOptionDto dto) {
        WorkOrderOption option = repository.findById(id).orElseThrow(() -> new RuntimeException("选项不存在"));
        copy(dto, option);
        return repository.save(option);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void seed(String category, String value, String label, int sortOrder) {
        if (repository.existsByCategoryAndValue(category, value)) {
            for (WorkOrderOption option : repository.findByCategoryOrderBySortOrderAscIdAsc(category)) {
                if (value.equals(option.getValue())) {
                    option.setLabel(label);
                    option.setSortOrder(sortOrder);
                    option.setEnabled(true);
                    repository.save(option);
                    return;
                }
            }
        } else {
            WorkOrderOption option = new WorkOrderOption();
            option.setCategory(category);
            option.setValue(value);
            option.setLabel(label);
            option.setSortOrder(sortOrder);
            option.setEnabled(true);
            repository.save(option);
        }
    }

    private void validate(WorkOrderOptionDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getCategory())) {
            throw new RuntimeException("选项分类不能为空");
        }
        if (!StringUtils.hasText(dto.getValue())) {
            throw new RuntimeException("选项值不能为空");
        }
        if (!StringUtils.hasText(dto.getLabel())) {
            throw new RuntimeException("选项名称不能为空");
        }
    }

    private void copy(WorkOrderOptionDto dto, WorkOrderOption option) {
        if (dto.getCategory() != null) option.setCategory(dto.getCategory().trim());
        if (dto.getValue() != null) option.setValue(dto.getValue().trim());
        if (dto.getLabel() != null) option.setLabel(dto.getLabel().trim());
        if (dto.getSortOrder() != null) option.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) option.setEnabled(dto.getEnabled());
    }
}
