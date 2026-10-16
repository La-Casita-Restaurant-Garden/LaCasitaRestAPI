package com.service.lacasita.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("No se pudo leer del caché '{}' (clave {}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("No se pudo escribir en el caché '{}' (clave {}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("No se pudo invalidar el caché '{}' (clave {}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("No se pudo limpiar el caché '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }

    private static final Map<String, Duration> CACHE_TTLS = Map.ofEntries(

            Map.entry("customersPage", Duration.ofMinutes(10)),
            Map.entry("customerDetail", Duration.ofMinutes(10)),
            Map.entry("salesChart", Duration.ofMinutes(15)),
            Map.entry("salesChartDaily", Duration.ofMinutes(10)),
            Map.entry("dashboardCharts", Duration.ofMinutes(15)),
            Map.entry("dashboardKpis", Duration.ofMinutes(5)),
            Map.entry("products", Duration.ofMinutes(10)),
            Map.entry("menuProducts", Duration.ofMinutes(10)),
            Map.entry("categories", Duration.ofMinutes(30)),
            Map.entry("categoriesInventory", Duration.ofMinutes(30)),
            Map.entry("categoriesVisibles", Duration.ofMinutes(30)),
            Map.entry("sales", Duration.ofMinutes(5)),
            Map.entry("userPermissions", Duration.ofMinutes(30)),
            Map.entry("rolePermissions", Duration.ofMinutes(30)),
            Map.entry("quickNotes", Duration.ofMinutes(30)));

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        CACHE_TTLS.forEach((name, ttl) -> cacheConfigurations.put(name, defaultCacheConfig.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Cache en memoria, para levantar sin Redis.
     */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
    public CacheManager inMemoryCacheManager() {
        log.info("Redis deshabilitado (app.redis.enabled=false): usando caché en memoria. "
                + "No se comparte entre instancias y se pierde al reiniciar.");
        return new ConcurrentMapCacheManager(CACHE_TTLS.keySet().toArray(String[]::new));
    }
}
