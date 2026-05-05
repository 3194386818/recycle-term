package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.DeviceType;
import com.xiaohei.recycle.repository.DeviceTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTypeService {

    private final DeviceTypeRepository repository;

    @PostConstruct
    public void init() {
        String[] defaults = {"光猫", "路由器", "FTTR主", "FTTR从", "IPTV", "OTT", "组网"};
        for (String name : defaults) {
            if (!repository.existsByName(name)) {
                DeviceType dt = new DeviceType();
                dt.setName(name);
                repository.save(dt);
            }
        }
    }

    public List<DeviceType> getAll(String sortBy, String sortOrder) {
        String actualSortBy = switch (sortBy) {
            case "name", "createdAt", "id" -> sortBy;
            default -> "id";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return repository.findAll(Sort.by(direction, actualSortBy));
    }

    public List<DeviceType> getAll() {
        return getAll("id", "desc");
    }

    @Transactional
    public DeviceType create(String name) {
        if (repository.existsByName(name)) {
            throw new RuntimeException("设备类型已存在");
        }
        DeviceType dt = new DeviceType();
        dt.setName(name);
        return repository.save(dt);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
