package com.hospital.config;

import com.hospital.service.ScheduleService;
import com.hospital.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时预热排班号源到 Redis。
 * Redis 不可用时直接启动失败，避免"看起来正常但一挂号就报错"。
 */
@Slf4j
@Component
public class RedisStockInitializer implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;
    private final ScheduleService scheduleService;
    private final RedisUtil redisUtil;

    public RedisStockInitializer(StringRedisTemplate redisTemplate,
                                 ScheduleService scheduleService,
                                 RedisUtil redisUtil) {
        this.redisTemplate = redisTemplate;
        this.scheduleService = scheduleService;
        this.redisUtil = redisUtil;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis 连接正常：{}", pong);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "无法连接 Redis，请先启动 Redis 服务（tools/redis/redis-server.exe）后再启动本应用", e);
        }
        // 清空历史号源，避免与数据库对不上
        java.util.Set<String> oldKeys = redisUtil.keys("hospital:schedule:stock:*");
        if (oldKeys != null && !oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
        }
        long start = System.currentTimeMillis();
        int count = scheduleService.warmUpStock();
        log.info("预热号源 {} 条，耗时 {} ms", count, System.currentTimeMillis() - start);
    }
}
