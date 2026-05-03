package com.xiaohei.recycle.config;

import com.xiaohei.recycle.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class EngineerInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        String token = auth.substring(7);
        try {
            Long engineerId = jwtUtil.getId(token);
            String phone = jwtUtil.getUsername(token);
            request.setAttribute("engineerId", engineerId);
            request.setAttribute("engineerPhone", phone);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }
}
