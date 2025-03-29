package com.example.demo.service;

import com.example.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisExampleService {

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 示例：使用Redis缓存用户信息
     */
    public void cacheUserInfo(String userId, Object userInfo) {
        String key = "user:" + userId;
        // 设置用户信息，过期时间为1小时
        redisUtils.set(key, userInfo, 1, TimeUnit.HOURS);
    }

    /**
     * 示例：使用Redis实现访问计数
     */
    public Long incrementVisitCount(String pageId) {
        String key = "visit:" + pageId;
        return redisUtils.incr(key, 1);
    }

    /**
     * 示例：使用Redis实现限流
     */
    public boolean rateLimit(String key, int limit, int seconds) {
        Long count = redisUtils.incr(key, 1);
        if (count == 1) {
            redisUtils.expire(key, seconds, TimeUnit.SECONDS);
        }
        return count <= limit;
    }

    /**
     * 示例：使用Redis存储用户会话
     */
    public void storeUserSession(String sessionId, Object sessionData) {
        String key = "session:" + sessionId;
        redisUtils.set(key, sessionData, 30, TimeUnit.MINUTES);
    }

    /**
     * 示例：使用Redis实现排行榜
     */
    public void addToLeaderboard(String leaderboardKey, String userId, double score) {
        redisUtils.zSet(leaderboardKey, userId, score);
    }

    /**
     * 示例：使用Redis实现消息队列
     */
    public void addToQueue(String queueKey, Object message) {
        redisUtils.lSet(queueKey, message);
    }

    /**
     * 示例：使用Redis实现标签系统
     */
    public void addTag(String tagKey, String itemId) {
        redisUtils.sSet(tagKey, itemId);
    }

    /**
     * 示例：使用Redis存储用户配置
     */
    public void storeUserConfig(String userId, String configKey, Object configValue) {
        String key = "config:" + userId;
        redisUtils.hSet(key, configKey, configValue);
    }
} 