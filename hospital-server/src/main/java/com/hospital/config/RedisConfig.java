package com.hospital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis Lua 脚本：保证"判断 + 扣减"与"比对 + 释放锁"两步原子执行
 */
@Configuration
public class RedisConfig {

    /**
     * 号源扣减：返回 -2 表示 key 不存在（需从 MySQL 回填），-1 表示号源已耗尽，>=0 为扣减后的余量
     */
    @Bean
    public DefaultRedisScript<Long> stockDecrScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local stock = redis.call('GET', KEYS[1])\n" +
                "if not stock then return -2 end\n" +
                "if tonumber(stock) <= 0 then return -1 end\n" +
                "return redis.call('DECR', KEYS[1])");
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 释放分布式锁：只有持有者（value 相同）才能删除
     */
    @Bean
    public DefaultRedisScript<Long> releaseLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                "return redis.call('DEL', KEYS[1]) else return 0 end");
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 号源回滚：异常时把已扣的号加回去
     */
    @Bean
    public DefaultRedisScript<Long> stockIncrScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                "return redis.call('INCR', KEYS[1]) else return -1 end");
        script.setResultType(Long.class);
        return script;
    }
}
