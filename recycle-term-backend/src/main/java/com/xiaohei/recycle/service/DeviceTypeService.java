package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.DeviceType;
import com.xiaohei.recycle.repository.DeviceTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
        if ("id".equalsIgnoreCase(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                return repository.findAllByIdOrderByIdAsc();
            } else {
                return repository.findAllByIdOrderByIdDesc();
            }
        } else if ("name".equalsIgnoreCase(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                return repository.findAllByOrderByNameAsc();
            } else {
                return repository.findAllByOrderByNameDesc();
            }
        } else if ("createdAt".equalsIgnoreCase(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                return repository.findAllByOrderByCreatedAtAsc();
            } else {
                return repository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            // 默认按ID降序排列（最新ID在前）
            return repository.findAllByIdOrderByIdDesc();
        }
    }

    public List<DeviceType> getAll() {
        return getAll("id", "desc"); // 默认按ID降序
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
