package com.xiaohei.recycle.service;

import com.xiaohei.recycle.entity.Engineer;
import com.xiaohei.recycle.repository.EngineerRepository;
import com.xiaohei.recycle.repository.RecycleTaskRepository;
import com.xiaohei.recycle.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EngineerService {

    private final EngineerRepository engineerRepository;
    private final RecycleTaskRepository taskRepository;
    private final JwtUtil jwtUtil;

    public Map<String, String> login(String phone, String password) {
        Engineer engineer = engineerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (!hash(password).equals(engineer.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        String token = jwtUtil.generate(engineer.getId(), phone);
        return Map.of("token", token, "phone", phone, "name", engineer.getName() != null ? engineer.getName() : "");
    }

    @Transactional
    public void changePassword(Long engineerId, String oldPassword, String newPassword) {
        Engineer engineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (!hash(oldPassword).equals(engineer.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        engineer.setPassword(hash(newPassword));
        engineerRepository.save(engineer);
    }

    public Page<Engineer> getEngineers(Pageable pageable) {
        return engineerRepository.findAll(pageable);
    }

    @Transactional
    public Engineer createEngineer(String phone, String name) {
        if (engineerRepository.existsByPhone(phone)) {
            throw new RuntimeException("手机号已存在");
        }
        Engineer engineer = new Engineer();
        engineer.setPhone(phone);
        engineer.setName(name);
        engineer.setPassword(hash("admin123"));
        return engineerRepository.save(engineer);
    }

    @Transactional
    public void deleteEngineer(Long id) {
        engineerRepository.deleteById(id);
    }

    @Transactional
    public void resetPassword(Long id) {
        Engineer engineer = engineerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        engineer.setPassword(hash("admin123"));
        engineerRepository.save(engineer);
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("recycle-salt:" + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败");
        }
    }
}
