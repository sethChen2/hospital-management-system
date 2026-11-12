package com.hospital.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.Constants;
import com.hospital.common.R;
import com.hospital.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 应用内限流：对提交类接口按账号做固定窗口计数（替代 Spring Cloud Gateway 限流）
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Value("${hospital.rate-limit.window-seconds}")
    private long windowSeconds;

    @Value("${hospital.rate-limit.max-times}")
    private int maxTimes;

    public RateLimitInterceptor(RedisUtil redisUtil, ObjectMapper objectMapper) {
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String account = (String) request.getAttribute(Constants.CLAIM_ACCOUNT);
        if (account == null) {
            return true;
        }
        String key = Constants.REDIS_LIMIT_PREFIX + request.getRequestURI() + ":" + account;
        Long count = redisUtil.increment(key);
        if (count != null && count == 1L) {
            redisUtil.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > maxTimes) {
            log.warn("触发限流：account={}, uri={}, count={}", account, request.getRequestURI(), count);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.error(429, "操作过于频繁，请 " + windowSeconds + " 秒后再试")));
            return false;
        }
        return true;
    }
}
