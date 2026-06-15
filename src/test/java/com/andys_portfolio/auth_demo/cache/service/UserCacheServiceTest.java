package com.andys_portfolio.auth_demo.cache.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andys_portfolio.auth_demo.cache.dto.UserAuthCache;
import com.andys_portfolio.auth_demo.database.user.entity.Role;
import com.andys_portfolio.auth_demo.database.user.entity.User;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
class UserCacheServiceTest {

  private static final String EMAIL = "cache-test@example.com";
  private static final String REDIS_CACHE_KEY = "userAuth::" + EMAIL;

  @Autowired
  private UserCacheService userCacheService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
    redisTemplate.delete(REDIS_CACHE_KEY);
  }

  @Test
  void getCachedUserLoadsUserAndStoresInRedis() {
    saveUser();

    UserAuthCache cached = userCacheService.getCachedUser(EMAIL);

    assertThat(cached.getEmail()).isEqualTo(EMAIL);
    assertThat(cached.getUsername()).isEqualTo(EMAIL);
    assertThat(cached.getPassword()).isEqualTo("encoded-password");
    assertThat(cached.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("USER");
    assertThat(redisTemplate.hasKey(REDIS_CACHE_KEY)).isTrue();
  }

  @Test
  void getCachedUserServesFromCacheWhenDatabaseEntryRemoved() {
    saveUser();
    userCacheService.getCachedUser(EMAIL);

    userRepository.deleteAll();

    UserAuthCache cached = userCacheService.getCachedUser(EMAIL);

    assertThat(cached.getEmail()).isEqualTo(EMAIL);
    assertThat(cached.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("USER");
  }

  @Test
  void evictUserRemovesCachedEntryFromRedis() {
    saveUser();
    userCacheService.getCachedUser(EMAIL);
    assertThat(redisTemplate.hasKey(REDIS_CACHE_KEY)).isTrue();

    userCacheService.evictUser(EMAIL);

    assertThat(redisTemplate.hasKey(REDIS_CACHE_KEY)).isFalse();
  }

  @Test
  void getCachedUserThrowsWhenUserNotFound() {
    assertThatThrownBy(() -> userCacheService.getCachedUser(EMAIL))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  private void saveUser() {
    userRepository.save(User.builder()
        .email(EMAIL)
        .password("encoded-password")
        .role(Role.USER)
        .build());
  }
}
