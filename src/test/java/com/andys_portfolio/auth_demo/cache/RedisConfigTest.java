package com.andys_portfolio.auth_demo.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.cache.type=redis"
})
class RedisConfigTest {

  private static final String CACHE_NAME = "testCache";
  private static final String CACHE_KEY = "user@example.com";

  @Autowired
  private RedisCacheConfiguration cacheConfiguration;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @AfterEach
  void tearDown() {
    Set<String> keys = redisTemplate.keys(CACHE_NAME + "::*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  @Test
  void cacheConfigurationHasThirtyMinuteTtl() {
    assertThat(cacheConfiguration.getTtl()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void cacheConfigurationDisablesNullValues() {
    assertThat(cacheConfiguration.getAllowCacheNullValues()).isFalse();
  }

  @Test
  void cacheManagerIsRedisBacked() {
    assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
  }

  @Test
  void cacheStoresAndRetrievesEntriesInRedis() {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    assertThat(cache).isNotNull();

    cache.put(CACHE_KEY, "cached-value");

    assertThat(cache.get(CACHE_KEY, String.class)).isEqualTo("cached-value");
    assertThat(redisTemplate.hasKey(CACHE_NAME + "::" + CACHE_KEY)).isTrue();
  }
}
