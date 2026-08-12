package com.regisx001.dQul.logs.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * Configures Redis-backed Spring Cache with JSON serialization.
 *
 * <p>
 * Values (e.g. {@link com.regisx001.dQul.logs.domain.LogEntry}, which contains
 * {@link java.time.Instant} fields) are serialized to JSON via a polymorphic
 * Jackson serializer so they deserialize back with the correct concrete type.
 * Keys are stored as prefixed strings ({@code dqul-logs:<cache>:<key>}) so
 * cache
 * namespaces and cache generations are easy to inspect and evict in Redis.
 */
@Configuration
public class CacheConfig {

    /** Cache for a single log entry keyed by its UUID. */
    public static final String CACHE_LOG_BY_ID = "logById";

    /** Cache for the aggregated statistics dashboard payload. */
    public static final String CACHE_LOG_STATS = "logStats";

    /** Cache for the full aggregated analytics dashboard envelope. */
    public static final String CACHE_LOG_ANALYTICS = "logAnalytics";

    /**
     * Cache for paginated query results keyed by the normalized (search, level,
     * serviceName, category, traceId, page, size) tuple.
     */
    public static final String CACHE_LOG_QUERY = "logQuery";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheConfiguration cacheConfiguration(ObjectMapper objectMapper) {
        // Trust only our own package for polymorphic typing. This keeps JSON
        // values self-describing (type + fields) while bounding the trusted types.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.regisx001.dQul.logs.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build();

        ObjectMapper cacheMapper = objectMapper.copy();
        cacheMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "dqul-logs:" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        valueSerializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultConfiguration) {
        return builder -> builder
                .withCacheConfiguration(CACHE_LOG_BY_ID, defaultConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_LOG_STATS, defaultConfiguration.entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration(CACHE_LOG_ANALYTICS, defaultConfiguration.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration(CACHE_LOG_QUERY, defaultConfiguration.entryTtl(DEFAULT_TTL));
    }

    /**
     * Primary cache manager wired into the Spring Cache abstraction. The
     * {@link RedisCacheManager} is built explicitly with our per-cache customizer
     * so each cache name (logById, logStats, logQuery) gets its own TTL.
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration defaultConfiguration,
            RedisCacheManagerBuilderCustomizer customizer) {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration);
        customizer.customize(builder);
        return builder.build();
    }

    @Bean
    @org.springframework.context.annotation.Profile("!test")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
