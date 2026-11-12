package com.hospital.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作封装（字符串模板，值与号源都按字符串存）
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate template;

    public RedisUtil(StringRedisTemplate template) {
        this.template = template;
    }

    public void set(String key, String value) {
        template.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        template.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return template.opsForValue().get(key);
    }

    public Boolean hasKey(String key) {
        return template.hasKey(key);
    }

    public Boolean delete(String key) {
        return template.delete(key);
    }

    public Long increment(String key) {
        return template.opsForValue().increment(key);
    }

    public Long decrement(String key) {
        return template.opsForValue().decrement(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return template.expire(key, timeout, unit);
    }

    public Set<String> keys(String pattern) {
        return template.keys(pattern);
    }

    /** 执行返回 Long 的 Lua 脚本 */
    public Long execute(RedisScript<Long> script, String key, Object... args) {
        return template.execute(script, Collections.singletonList(key), args);
    }

    /** SET key value NX PX 抢占分布式锁 */
    public boolean tryLock(String key, String value, long expireMillis) {
        Boolean ok = template.opsForValue()
                .setIfAbsent(key, value, expireMillis, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(ok);
    }
}
