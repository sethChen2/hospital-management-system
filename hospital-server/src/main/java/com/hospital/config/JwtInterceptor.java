package com.hospital.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.Constants;
import com.hospital.common.R;
import com.hospital.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：解析请求头 token，把 userId / role 放进 request 属性
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // CORS 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(Constants.TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            writeUnauthorized(response, "未登录或登录已过期，请先登录");
            return false;
        }

        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            String role = jwt.getClaim(Constants.CLAIM_ROLE).asString();
            String required = requiredRole(request.getRequestURI());
            if (required != null && !required.equals(role)) {
                log.warn("越权访问：role={}, uri={}", role, request.getRequestURI());
                writeError(response, 403, "无权访问该接口");
                return false;
            }
            request.setAttribute(Constants.CLAIM_USER_ID,
                    jwt.getClaim(Constants.CLAIM_USER_ID).asInt());
            request.setAttribute(Constants.CLAIM_ROLE, role);
            request.setAttribute(Constants.CLAIM_ACCOUNT,
                    jwt.getClaim(Constants.CLAIM_ACCOUNT).asString());
            return true;
        } catch (JWTVerificationException e) {
            log.warn("token 校验失败：{}", e.getMessage());
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    /** 路径前缀决定所需角色，/user/** 各角色通用 */
    private String requiredRole(String uri) {
        if (uri.startsWith("/admin/")) {
            return Constants.ROLE_ADMIN;
        }
        if (uri.startsWith("/doctor/")) {
            return Constants.ROLE_DOCTOR;
        }
        if (uri.startsWith("/patient/")) {
            return Constants.ROLE_PATIENT;
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        writeError(response, 401, msg);
    }

    /** HTTP 状态保持 200，业务状态放 body.status（与其他接口一致） */
    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.error(status, msg)));
    }
}
