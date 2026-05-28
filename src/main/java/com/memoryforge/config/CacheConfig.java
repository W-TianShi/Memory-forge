package com.memoryforge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager("wordInfo", "wordBatch");
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(24, TimeUnit.HOURS));
        return mgr;
    }
}
