package com.internship.tool.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configures Redis as the cache provider.
 * Cache entries automatically expire after 10 minutes (TTL).
 * AI responses use 15 minutes TTL (set in AiServiceClient).
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a CacheManager backed by Redis.
     * All @Cacheable methods use this manager automatically.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))   // cache entries live 10 minutes
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()  // store as JSON
                        )
                )
                .disableCachingNullValues();  // never cache null results

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}