package com.zjgsu.whattoeat.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置。
 * <p>
 * 使用 Caffeine 为频繁读取、低频写入的数据提供内存缓存，
 * 减少数据库查询压力，降低列表接口 P99 响应时间。
 */
@Configuration
public class CacheConfig {

    /**
     * 餐厅聚合快照缓存。
     * <ul>
     *   <li>TTL: 5 分钟（写入后过期）</li>
     *   <li>最大条目: 1000</li>
     *   <li>评论写入/更新时主动驱逐对应 key</li>
     * </ul>
     */
    @Bean
    public Cache<String, RestaurantMetricSnapshotEntity> snapshotCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }
}
