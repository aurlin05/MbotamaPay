package com.mbotamapay.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
         * Custom ObjectMapper for Redis serialization.
         * Configured to handle JPA entities, circular references, and Java 8 date/time
         * types.
         */
        @Bean(name = "redisObjectMapper")
        public ObjectMapper redisObjectMapper() {
                ObjectMapper objectMapper = new ObjectMapper();

                // Register JavaTimeModule for LocalDateTime support
                objectMapper.registerModule(new JavaTimeModule());

                // Disable writing dates as timestamps
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // Fail on empty beans is disabled to handle proxy objects
                objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

                // Note: Default typing is NOT enabled to avoid conflicts with HTTP requests
                // Cache will store simple POJOs without type information

                return objectMapper;
        }

        /**
         * RedisTemplate bean for general Redis operations.
         * Used by RedisIdempotencyService and other services that need direct Redis
         * access.
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                        ObjectMapper redisObjectMapper) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Use String serializer for keys
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Use custom JSON serializer with configured ObjectMapper
                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper);
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
        public RedisCacheConfiguration defaultCacheConfiguration(ObjectMapper redisObjectMapper) {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer(
                                                                                redisObjectMapper)));
        }

        /**
         * User cache configuration with 30-minute TTL.
         * User data changes infrequently, so longer TTL is appropriate.
         */
        @Bean
        public RedisCacheConfiguration userCacheConfiguration(ObjectMapper redisObjectMapper) {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer(
                                                                                redisObjectMapper)));
        }

        /**
         * Wallet cache configuration with 5-minute TTL.
         * Wallet balances change frequently, so shorter TTL is needed.
         */
        @Bean
        public RedisCacheConfiguration walletCacheConfiguration(ObjectMapper redisObjectMapper) {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer(
                                                                                redisObjectMapper)));
        }

        /**
         * Transaction history cache configuration with 10-minute TTL.
         * Transaction history is read-heavy but updates moderately.
         */
        @Bean
        public RedisCacheConfiguration transactionHistoryCacheConfiguration(ObjectMapper redisObjectMapper) {
                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer(
                                                                                redisObjectMapper)));
        }

        /**
         * Cache manager with per-cache TTL configurations.
         * Uses LRU eviction policy (default in Redis).
         */
        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Configure specific caches with their TTLs
                cacheConfigurations.put("users", userCacheConfiguration(redisObjectMapper));
                cacheConfigurations.put("wallets", walletCacheConfiguration(redisObjectMapper));
                cacheConfigurations.put("walletBalance", walletCacheConfiguration(redisObjectMapper));
                cacheConfigurations.put("transactionHistory", transactionHistoryCacheConfiguration(redisObjectMapper));
                cacheConfigurations.put("adminStats", defaultCacheConfiguration(redisObjectMapper));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultCacheConfiguration(redisObjectMapper))
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}
