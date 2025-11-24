package com.mbotamapay.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

        /**
         * RedisTemplate bean for general Redis operations.
         * Used by RedisIdempotencyService and other services that need direct Redis
         * access.
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Use String serializer for keys
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Use JSON serializer for values
                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
                template.setValueSerializer(jsonSerializer);
                template.setHashValueSerializer(jsonSerializer);

                template.afterPropertiesSet();
                return template;
        }

        /**
         * Default cache configuration with 5-minute TTL.
         * Used as fallback for caches without specific configuration.
         */
        @Bean
        public RedisCacheConfiguration defaultCacheConfiguration() {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer()));
        }

        /**
         * User cache configuration with 30-minute TTL.
         * User data changes infrequently, so longer TTL is appropriate.
         */
        @Bean
        public RedisCacheConfiguration userCacheConfiguration() {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer()));
        }

        /**
         * Wallet cache configuration with 5-minute TTL.
         * Wallet balances change frequently, so shorter TTL is needed.
         */
        @Bean
        public RedisCacheConfiguration walletCacheConfiguration() {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer()));
        }

        /**
         * Transaction history cache configuration with 10-minute TTL.
         * Transaction history is read-heavy but updates moderately.
         */
        @Bean
        public RedisCacheConfiguration transactionHistoryCacheConfiguration() {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer()));
        }

        /**
         * Cache manager with per-cache TTL configurations.
         * Uses LRU eviction policy (default in Redis).
         */
        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Configure specific caches with their TTLs
                cacheConfigurations.put("users", userCacheConfiguration());
                cacheConfigurations.put("wallets", walletCacheConfiguration());
                cacheConfigurations.put("walletBalance", walletCacheConfiguration());
                cacheConfigurations.put("transactionHistory", transactionHistoryCacheConfiguration());
                cacheConfigurations.put("adminStats", defaultCacheConfiguration());

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultCacheConfiguration())
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}
